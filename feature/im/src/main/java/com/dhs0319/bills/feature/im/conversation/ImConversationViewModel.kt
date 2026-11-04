package com.dhs0319.bills.feature.im.conversation

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.comment.CommentEmoteRepository
import com.dhs0319.bills.core.comment.ImageAttachmentHandler
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.im.ImRepository
import com.dhs0319.bills.core.im.ImImageUploader
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.ImConversationPage
import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.core.model.ImUploadedImage
import com.dhs0319.bills.feature.comment.editor.deleteEmoteBeforeCursor
import com.dhs0319.bills.feature.comment.editor.insertEmote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class ImConversationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imRepo: ImRepository,
    private val authRepo: AuthRepository,
    private val emoteRepo: CommentEmoteRepository,
    private val imageHandler: ImageAttachmentHandler,
    private val imageUploader: ImImageUploader
) : ViewModel() {

    private val talkerId = savedStateHandle.get<Long>("talkerId") ?: 0L
    private val sessionType = savedStateHandle.get<Int>("sessionType") ?: 0
    private val routeTitle = savedStateHandle.get<String>("title").orEmpty()
    private val routeAvatar = savedStateHandle.get<String>("avatar")?.takeIf(String::isNotBlank)
    private val latestMessageIsAutoReply = savedStateHandle.get<Boolean>("autoReply") ?: false
    private var autoReplyMessageKey: Long? = null
    private var emoteJob: Job? = null
    private var refreshJob: Job? = null
    private var uploadedImage: Pair<Uri, ImUploadedImage>? = null
    private val rowCache = ConversationRowCache()
    private val timelineMutex = Mutex()
    private val recentWrites = Channel<Pair<Long, List<CommentEmote>>>(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(ImConversationUiState())
    internal val uiState: StateFlow<ImConversationUiState> = _uiState.asStateFlow()
    private val _composerState = MutableStateFlow(ImConversationComposerState())
    internal val composerState: StateFlow<ImConversationComposerState> = _composerState.asStateFlow()

    init {
        _uiState.value = ImConversationUiState(
            title = routeTitle,
            avatar = routeAvatar,
            selfAvatar = authRepo.getUserInfo()?.avatar?.takeIf(String::isNotBlank)
        )
        _composerState.value = ImConversationComposerState(
            input = savedStateHandle.get<String>(DRAFT_TEXT)?.let { TextFieldValue(it, TextRange(it.length)) }
                ?: TextFieldValue(),
            selectedImageUri = savedStateHandle.get<String>(DRAFT_IMAGE)?.let(Uri::parse)
        )
        viewModelScope.launch {
            for ((mid, recent) in recentWrites) {
                try {
                    emoteRepo.persistRecent(mid, recent)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Logger.e(TAG, error) { "save recent im emotes failed" }
                }
            }
        }
        refresh()
        loadEmotes()
    }

    fun refresh() {
        val state = _uiState.value
        if (refreshJob?.isActive == true || state.isRefreshing || state.isLoadingMore) return
        _uiState.update {
            it.copy(
                isLoading = it.messages.isEmpty(),
                isRefreshing = it.messages.isNotEmpty()
            )
        }
        refreshJob = viewModelScope.launch {
            try {
                val page = imRepo.fetchConversation(
                    talkerId = talkerId,
                    sessionType = sessionType
                )
                applyConversation(page)
                reportAck(page.messages.maxOfOrNull { it.seqNo })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMoreHistory) return
        val beforeSeqNo = state.messages.lastOrNull()?.seqNo ?: return
        _uiState.update {
            it.copy(
                isLoadingMore = true
            )
        }
        viewModelScope.launch {
            try {
                val page = imRepo.fetchOlderMessages(
                    talkerId = talkerId,
                    sessionType = sessionType,
                    beforeSeqNo = beforeSeqNo
                )
                applyOlderMessages(page)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun updateInput(input: TextFieldValue) {
        val state = _composerState.value
        if (state.isSending || state.input == input) return
        val textChanged = input.text != state.input.text
        if (textChanged) savedStateHandle[DRAFT_TEXT] = input.text
        _composerState.update { it.copy(input = input, sendErrorMessage = if (textChanged) null else it.sendErrorMessage) }
    }

    fun selectImage(uri: Uri?) {
        if (_composerState.value.isSending) return
        uploadedImage = null
        savedStateHandle[DRAFT_IMAGE] = uri?.toString()
        _composerState.update { it.copy(selectedImageUri = uri, sendErrorMessage = null) }
    }

    fun loadEmotes(force: Boolean = false) {
        if (emoteJob?.isActive == true || (!force && _composerState.value.emotePicker.packages.isNotEmpty())) return
        _composerState.update { it.copy(emotePicker = it.emotePicker.copy(loading = true, error = null)) }
        emoteJob = viewModelScope.launch {
            try {
                val packages = emoteRepo.fetchPackages(force)
                val recent = withContext(Dispatchers.IO) { emoteRepo.recent(packages) }
                publishEmotes(packages.flatMap { it.emotes })
                _composerState.update { state ->
                    state.copy(
                        emotePicker = state.emotePicker.copy(
                            loading = false,
                            packages = packages,
                            recent = recent,
                            selectedPackageId = state.emotePicker.selectedPackageId.takeIf { selected ->
                                selected == -1L || packages.any { it.id == selected }
                            } ?: packages.first().id
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _composerState.update {
                    it.copy(emotePicker = it.emotePicker.copy(loading = false, error = error.message ?: "表情加载失败"))
                }
            }
        }
    }

    fun selectEmotePackage(id: Long) {
        _composerState.update { it.copy(emotePicker = it.emotePicker.copy(selectedPackageId = id)) }
    }

    fun insertEmote(emote: CommentEmote) {
        val state = _composerState.value
        if (state.isSending || !emote.available) return
        updateInput(state.input.insertEmote(emote, state.emotes))
        val recent = emoteRepo.updatedRecent(emote, state.emotePicker.recent)
        _composerState.update { it.copy(emotePicker = it.emotePicker.copy(recent = recent)) }
        recentWrites.trySend(authRepo.currentMidFlow.value to recent)
    }

    fun deleteText() {
        val state = _composerState.value
        updateInput(state.input.deleteEmoteBeforeCursor(state.emotes))
    }

    fun sendMessage() {
        val state = _composerState.value
        val text = state.input.text.trimEnd()
        val imageUri = state.selectedImageUri
        if (state.isSending || (text.isBlank() && imageUri == null)) return
        if (talkerId <= 0L || sessionType <= 0) {
            _composerState.update { it.copy(sendErrorMessage = "会话信息无效") }
            return
        }
        _composerState.update { it.copy(isSending = true, sendErrorMessage = null) }
        viewModelScope.launch {
            try {
                if (text.isNotBlank()) {
                    val message = imRepo.sendConversationMessage(talkerId, sessionType, text)
                    savedStateHandle[DRAFT_TEXT] = ""
                    appendSentMessage(message)
                    _composerState.update { it.copy(input = TextFieldValue()) }
                }
                if (imageUri != null) {
                    val image = uploadedImage?.takeIf { it.first == imageUri }?.second ?: run {
                        _composerState.update { it.copy(isUploadingImage = true) }
                        val prepared = imageHandler.prepare(imageUri)
                        imageUploader.upload(prepared.bytes, prepared.mimeType).also {
                            uploadedImage = imageUri to it
                        }
                    }
                    _composerState.update { it.copy(isUploadingImage = false) }
                    val message = imRepo.sendConversationImage(talkerId, sessionType, image)
                    savedStateHandle[DRAFT_IMAGE] = null
                    uploadedImage = null
                    appendSentMessage(message)
                    _composerState.update { it.copy(selectedImageUri = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e(TAG, error) { "send im message failed talkerId=$talkerId sessionType=$sessionType" }
                _composerState.update { it.copy(sendErrorMessage = error.message ?: "发送消息失败，请重试") }
            } finally {
                _composerState.update { it.copy(isSending = false, isUploadingImage = false) }
            }
        }
    }

    private suspend fun appendSentMessage(message: ImMessage) = updateMessages(
        transform = { (listOf(message) + it).distinctBy(ImMessage::key) },
        updateState = { it.copy(lastSentMessageKey = message.key) }
    )

    private suspend fun updateMessages(
        transform: (List<ImMessage>) -> List<ImMessage>,
        updateState: (ImConversationUiState) -> ImConversationUiState
    ) {
        timelineMutex.withLock {
            val current = _uiState.value.messages
            val (messages, rows) = withContext(Dispatchers.Default) {
                val messages = transform(current)
                messages to rowCache.build(messages, ZonedDateTime.now())
            }
            _uiState.update { updateState(it).copy(messages = messages, rows = rows) }
        }
    }

    private fun publishEmotes(incoming: List<CommentEmote>) {
        val emotes = mergeEmotes(_uiState.value.emotes, incoming)
        _uiState.update { it.copy(emotes = emotes) }
        _composerState.update { it.copy(emotes = emotes) }
    }

    private fun reportAck(seqNo: Long?) {
        val ackSeqNo = seqNo?.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            imRepo.updateAck(
                talkerId = talkerId,
                sessionType = sessionType,
                ackSeqNo = ackSeqNo
            )
        }
    }

    private suspend fun applyConversation(
        page: ImConversationPage
    ) {
        if (autoReplyMessageKey == null && latestMessageIsAutoReply) {
            autoReplyMessageKey = page.messages.firstOrNull()?.key
        }
        publishEmotes(page.emotes)
        updateMessages(
            transform = { page.messages.withAutoReplyMark() },
            updateState = { state -> state.copy(
                title = state.title.ifBlank { routeTitle },
                avatar = state.avatar ?: routeAvatar,
                hasMoreHistory = page.hasMoreHistory,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false
            ) }
        )
    }

    private suspend fun applyOlderMessages(page: ImConversationPage) {
        publishEmotes(page.emotes)
        updateMessages(
            transform = { (it + page.messages).distinctBy(ImMessage::key) },
            updateState = { state -> state.copy(
                hasMoreHistory = page.hasMoreHistory,
                isLoadingMore = false
            ) }
        )
    }

    private fun mergeEmotes(
        current: List<CommentEmote>,
        incoming: List<CommentEmote>
    ): List<CommentEmote> {
        if (incoming.isEmpty()) return current
        if (current.isEmpty()) return incoming
        val tokens = current.mapTo(HashSet(current.size)) { it.text }
        val additions = incoming.filter { tokens.add(it.text) }
        return if (additions.isEmpty()) current else current + additions
    }

    private fun List<ImMessage>.withAutoReplyMark(): List<ImMessage> {
        val key = autoReplyMessageKey ?: return this
        return map { message ->
            if (message.key == key && !message.isAutoReply) {
                message.copy(isAutoReply = true)
            } else {
                message
            }
        }
    }

    private companion object {
        const val TAG = "ImConversationViewModel"
        const val DRAFT_TEXT = "im_draft_text"
        const val DRAFT_IMAGE = "im_draft_image"
    }
}
