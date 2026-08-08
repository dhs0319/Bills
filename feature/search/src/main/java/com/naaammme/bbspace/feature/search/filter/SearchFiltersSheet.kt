package com.naaammme.bbspace.feature.search.filter

import android.app.DatePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naaammme.bbspace.core.model.SearchFilter
import com.naaammme.bbspace.core.model.SearchOp
import com.naaammme.bbspace.core.model.SearchTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFiltersSheet(
    filters: List<SearchFilter>,
    selectedMap: Map<String, Set<String>>,
    time: SearchTime,
    onDismiss: () -> Unit,
    onApply: (Map<String, Set<String>>, SearchTime) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftSel by remember(filters, selectedMap) {
        mutableStateOf(selectedMap.filterValues { it.isNotEmpty() })
    }
    var draftTime by remember(filters, selectedMap, time) {
        mutableStateOf(
            if (selectedMap[SINCE_KEY]?.singleOrNull() == CUSTOM_TIME) {
                time
            } else {
                SearchTime()
            }
        )
    }
    val canApply = draftSel[SINCE_KEY]?.singleOrNull() != CUSTOM_TIME || draftTime.isActive

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(
                key = "title",
                contentType = "title"
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            itemsIndexed(
                items = filters,
                key = { _, filter -> filter.key },
                contentType = { _, _ -> "filter" }
            ) { index, filter ->
                val picked = draftSel[filter.key].orEmpty()
                SearchFilterSection(
                    filter = filter,
                    picked = picked,
                    time = draftTime,
                    onToggle = { op ->
                        val nextSel = togglePick(filter, op, picked)
                        draftSel = draftSel.toMutableMap().apply {
                            if (nextSel.isEmpty()) remove(filter.key) else put(filter.key, nextSel)
                        }
                        if (filter.key == SINCE_KEY && nextSel.singleOrNull() != CUSTOM_TIME) {
                            draftTime = SearchTime()
                        }
                    },
                    onTimeChange = { nextTime ->
                        draftTime = nextTime
                    }
                )
                if (index != filters.lastIndex) {
                    HorizontalDivider()
                }
            }

            item(
                key = "actions",
                contentType = "actions"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            draftSel = emptyMap()
                            draftTime = SearchTime()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                    TextButton(
                        onClick = { onApply(draftSel, draftTime) },
                        enabled = canApply,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSection(
    filter: SearchFilter,
    picked: Set<String>,
    time: SearchTime,
    onToggle: (SearchOp) -> Unit,
    onTimeChange: (SearchTime) -> Unit
) {
    Column {
        Text(
            text = filter.title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (filter.single) "单选" else "多选",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filter.ops.forEach { op ->
                FilterChip(
                    selected = isPicked(op, picked),
                    onClick = { onToggle(op) },
                    label = { Text(op.label) }
                )
            }
        }
        if (filter.key == SINCE_KEY && picked.singleOrNull() == CUSTOM_TIME) {
            CustomTimePanel(
                time = time,
                onChange = onTimeChange
            )
        }
    }
}

@Composable
private fun CustomTimePanel(
    time: SearchTime,
    onChange: (SearchTime) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "自定义时间",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DateBtn(
                label = "开始日期",
                timeS = time.beginS,
                end = false,
                modifier = Modifier.weight(1f)
            ) { picked ->
                val end = when {
                    time.endS == 0L -> 0L
                    time.endS < picked -> endOfDay(picked)
                    else -> time.endS
                }
                onChange(SearchTime(beginS = picked, endS = end))
            }
            DateBtn(
                label = "结束日期",
                timeS = time.endS,
                end = true,
                modifier = Modifier.weight(1f)
            ) { picked ->
                val begin = when {
                    time.beginS == 0L -> 0L
                    time.beginS > picked -> startOfDay(picked)
                    else -> time.beginS
                }
                onChange(SearchTime(beginS = begin, endS = picked))
            }
        }

        Text(
            text = if (time.isActive) timeText(time) else "请选择开始和结束日期",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateBtn(
    label: String,
    timeS: Long,
    end: Boolean,
    modifier: Modifier = Modifier,
    onPicked: (Long) -> Unit
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance().apply {
                if (timeS > 0L) {
                    timeInMillis = timeS * 1000
                }
            }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, if (end) 23 else 0)
                        set(Calendar.MINUTE, if (end) 59 else 0)
                        set(Calendar.SECOND, if (end) 59 else 0)
                        set(Calendar.MILLISECOND, if (end) 999 else 0)
                    }
                    onPicked(picked.timeInMillis / 1000)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = modifier
    ) {
        Text(
            text = if (timeS > 0L) formatDay(timeS) else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun isPicked(op: SearchOp, picked: Set<String>): Boolean {
    return if (op.isDefault) picked.isEmpty() else op.param in picked
}

private fun togglePick(
    filter: SearchFilter,
    op: SearchOp,
    picked: Set<String>
): Set<String> {
    if (op.isDefault) return emptySet()
    if (filter.single) {
        return if (op.param in picked) emptySet() else setOf(op.param)
    }
    return if (op.param in picked) picked - op.param else picked + op.param
}

private fun timeText(time: SearchTime): String {
    return "${formatDay(time.beginS)} 至 ${formatDay(time.endS)}"
}

private fun formatDay(timeS: Long): String {
    return DateFormat.format("yyyy-MM-dd", timeS * 1000).toString()
}

private fun startOfDay(timeS: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeS * 1000
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 1000
}

private fun endOfDay(timeS: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeS * 1000
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis / 1000
}

internal const val SINCE_KEY = "since"
internal const val CUSTOM_TIME = "custom"
