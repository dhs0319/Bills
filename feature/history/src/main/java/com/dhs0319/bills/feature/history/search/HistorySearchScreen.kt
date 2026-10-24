package com.dhs0319.bills.feature.history.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AdaptiveMediaGrid
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.SearchCapsuleField
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.model.HistoryTarget
import com.dhs0319.bills.feature.history.component.HistoryItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySearchScreen(
    onBack: () -> Unit,
    onOpenHistoryTarget: (HistoryTarget?) -> Unit,
    viewModel: HistorySearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val columns = if (isTablet || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        2
    } else {
        1
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val keyboardOptions = remember {
        KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        )
    }
    val submitSearch = remember(focusManager, keyboard, viewModel) {
        {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            viewModel.submitSearch()
        }
    }
    val keyboardActions = remember(submitSearch) {
        KeyboardActions(onSearch = { submitSearch() })
    }

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = {
                    SearchCapsuleField(
                        value = state.input,
                        onValueChange = viewModel::updateKeyword,
                        placeholder = "搜索历史记录",
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboard?.hide()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = submitSearch
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.errorMessage != null && state.items.isEmpty() -> {
                    StateMessageCard(
                        text = state.errorMessage.orEmpty().ifBlank { "搜索历史记录失败" },
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        isError = true,
                        actionText = "重试",
                        onAction = viewModel::submitSearch
                    )
                }

                !state.isLoading && (state.query.isBlank() || state.items.isEmpty()) -> {
                    Text(
                        text = if (state.query.isBlank()) {
                            "输入关键词搜索历史记录"
                        } else {
                            "没有找到相关历史记录"
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(start = 24.dp, top = 96.dp, end = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    AdaptiveMediaGrid(
                        items = state.items,
                        isRefreshing = state.isLoading,
                        isLoadingMore = state.isLoadingMore,
                        onRefresh = submitSearch,
                        onLoadMore = viewModel::loadMore,
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        columns = columns,
                        loadMoreEnabled = state.canLoadMore && state.errorMessage == null,
                        prefetchScreens = 1,
                        loadMoreError = state.errorMessage,
                        onRetryLoadMore = viewModel::loadMore,
                        endReached = state.items.isNotEmpty() && !state.hasMore,
                        key = { _, item -> item.key },
                        contentType = { _, item -> item.type },
                        loadingPlaceholderCount = columns * 8,
                        loadingContent = {
                            HorizontalVideoCardSkeleton(isTablet = isTablet)
                        }
                    ) { item ->
                        HistoryItemCard(
                            item = item,
                            onClick = { onOpenHistoryTarget(item.target) }
                        )
                    }
                }
            }
        }
    }
}
