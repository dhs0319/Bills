package com.dhs0319.bills.feature.im.conversation

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.core.model.ImMsgType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Immutable
internal sealed interface ConversationRow {
    val key: Any
    val contentType: String
}

@Immutable
internal data class MessageRow(val item: ConversationMessageItem) : ConversationRow {
    override val key: Any get() = item.message.key
    override val contentType: String get() = item.contentType
}

@Immutable
internal data class TimeDividerRow(val messageKey: Long, val text: String) : ConversationRow {
    override val key: Any = "time$messageKey"
    override val contentType: String get() = "time"
}

@Immutable
internal data class ConversationMessageItem(
    val message: ImMessage,
    val displayText: String,
    val timeText: String,
    val imageRatio: Float,
    val contentType: String
)

/** Confined to the ViewModel's timeline mutex; retains only the currently loaded messages. */
internal class ConversationRowCache {
    private var date: LocalDate? = null
    private var zone: ZoneId? = null
    private var messagesByKey: Map<Long, MessageRow> = emptyMap()
    private var dividersByKey: Map<Long, TimeDividerRow> = emptyMap()

    fun build(messages: List<ImMessage>, now: ZonedDateTime): List<ConversationRow> {
        val today = now.toLocalDate()
        if (today != date || now.zone != zone) {
            messagesByKey = emptyMap()
            dividersByKey = emptyMap()
            date = today
            zone = now.zone
        }
        val yesterday = today.minusDays(1L)
        val nextMessages = HashMap<Long, MessageRow>(messages.size)
        val nextDividers = HashMap<Long, TimeDividerRow>()
        val rows = ArrayList<ConversationRow>(messages.size + 4)
        var newer: MessageRow? = null

        fun addDivider(row: MessageRow) {
            val text = row.item.timeText
            if (text.isEmpty()) return
            val key = row.item.message.key
            val divider = dividersByKey[key]?.takeIf { it.text == text } ?: TimeDividerRow(key, text)
            rows += divider
            nextDividers[key] = divider
        }

        for (message in messages) {
            val cached = messagesByKey[message.key]
            val row = cached?.takeIf { it.item.message == message } ?: MessageRow(
                ConversationMessageItem(
                    message = message,
                    displayText = message.displayText(),
                    timeText = formatMessageTime(message.timestampSec, now.zone, today, yesterday),
                    imageRatio = if (message.imageWidth > 0 && message.imageHeight > 0) {
                        (message.imageWidth.toFloat() / message.imageHeight).coerceIn(0.45f, 1.8f)
                    } else 1f,
                    contentType = when {
                        message.msgType == ImMsgType.SYSTEM_NOTICE -> "system_notice"
                        message.msgType == ImMsgType.NOTICE -> "notice"
                        !message.shareCoverUrl.isNullOrBlank() -> "share"
                        !message.imageUrl.isNullOrBlank() -> "image"
                        else -> "text"
                    }
                )
            )
            newer?.takeIf { it.item.timeText != row.item.timeText }?.let(::addDivider)
            rows += row
            nextMessages[message.key] = row
            newer = row
        }
        newer?.let(::addDivider)
        messagesByKey = nextMessages
        dividersByKey = nextDividers
        return rows
    }
}

private fun ImMessage.displayText(): String = content.takeIf(String::isNotBlank) ?: when (msgType) {
    ImMsgType.IMAGE -> "[图片]"
    in ImMsgType.SHARE_TYPES -> "[分享]"
    else -> "[暂不支持的消息类型 $msgType]"
}

private fun formatMessageTime(timestampSec: Long, zone: ZoneId, today: LocalDate, yesterday: LocalDate): String {
    if (timestampSec <= 0L) return ""
    val time = Instant.ofEpochSecond(timestampSec).atZone(zone)
    val date = time.toLocalDate()
    return when {
        date == today -> time.format(TIME_FORMAT_HM)
        date == yesterday -> "昨天 ${time.format(TIME_FORMAT_HM)}"
        date.year == today.year -> time.format(TIME_FORMAT_MD_HM)
        else -> time.format(TIME_FORMAT_YMD_HM)
    }
}

private val TIME_FORMAT_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val TIME_FORMAT_MD_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val TIME_FORMAT_YMD_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
