package com.dhs0319.bills.feature.search

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.SearchCapsuleField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchTopBar(
    text: String,
    autoFocus: Boolean,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenSpace: (Long) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val spaceUid = text.trim().toLongOrNull()?.takeIf { it > 0L }

    val imeVisible = WindowInsets.isImeVisible
    var imeShown by remember { mutableStateOf(false) }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeShown = true
        } else if (imeShown) {
            focusManager.clearFocus(force = true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    TopAppBar(
        title = {
            SearchCapsuleField(
                value = text,
                onValueChange = onTextChange,
                placeholder = "搜索视频",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        onSearch()
                    }
                )
            )
        },
        actions = {
            if (spaceUid != null) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        onOpenSpace(spaceUid)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "进入用户空间"
                    )
                }
            }
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                    onSearch()
                }
            ) {
                Text(
                    text = "搜索",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
            }
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
        scrollBehavior = scrollBehavior
    )
}
