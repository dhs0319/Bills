package com.dhs0319.bills.feature.settings.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AppUpdateDialog as CoreAppUpdateDialog
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.feature.settings.R
import com.dhs0319.bills.feature.settings.components.SettingSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToErrorLog: () -> Unit,
    versionName: String,
    vm: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val autoCheckUpdate by vm.autoCheckUpdate.collectAsStateWithLifecycle()
    val updateDialog by vm.updateDialog.collectAsStateWithLifecycle()

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.about_banner),
                            contentDescription = null,
                            modifier = Modifier.size(104.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = vm::checkUpdate,
                    enabled = updateState != UpdateState.Checking
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("检查更新", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = updateStatusLabel(updateState),
                            style = MaterialTheme.typography.bodySmall,
                            color = updateStatusColor(updateState)
                        )
                    }
                }
            }

            item {
                SettingSwitch(
                    title = "自动检查更新",
                    subtitle = "应用初始化时自动检查并弹出更新说明",
                    checked = autoCheckUpdate,
                    onCheckedChange = vm::updateAutoCheckEnabled
                )
            }

            item {
                LinkCard(
                    title = "错误日志",
                    subtitle = "查看和导出应用错误记录",
                    onClick = onNavigateToErrorLog
                )
            }

            item {
                LinkCard(
                    title = "GitHub 开源仓库",
                    subtitle = "github.com/dhs0319/Bills",
                    url = "https://github.com/dhs0319/Bills"
                )
            }
        }
    }

    updateDialog?.let { release ->
        CoreAppUpdateDialog(
            state = release,
            onDismiss = vm::dismissUpdateDialog,
            onOpenUrl = {
                vm.dismissUpdateDialog()
                context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
            }
        )
    }
}

@Composable
private fun LinkCard(
    title: String,
    subtitle: String,
    url: String? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedOnClick = onClick ?: { context.startActivity(Intent(Intent.ACTION_VIEW, url!!.toUri())) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = resolvedOnClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun updateStatusLabel(state: UpdateState): String = when (state) {
    is UpdateState.Idle -> "点击这里检查新版本"
    is UpdateState.Checking -> "正在检查..."
    is UpdateState.UpToDate -> "已是最新版本"
    is UpdateState.HasUpdate -> "发现新版本 v${state.version}"
    is UpdateState.Error -> "检查失败，点击重试"
}

@Composable
private fun updateStatusColor(state: UpdateState) = when (state) {
    is UpdateState.HasUpdate,
    is UpdateState.UpToDate -> MaterialTheme.colorScheme.primary
    is UpdateState.Error -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
