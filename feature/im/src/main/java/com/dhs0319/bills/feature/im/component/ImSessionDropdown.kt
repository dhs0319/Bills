package com.dhs0319.bills.feature.im.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.ImSessionTab

@Composable
internal fun ImSessionDropdown(
    tabs: List<ImSessionTab>,
    selectedTab: ImSessionTab,
    onSelect: (ImSessionTab) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedTab.title, style = MaterialTheme.typography.bodyMedium)
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 128.dp)
        ) {
            tabs.forEach { tab ->
                DropdownMenuItem(
                    text = { Text(tab.title, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(tab)
                    },
                    trailingIcon = {
                        if (tab == selectedTab) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.semantics { selected = tab == selectedTab }
                )
            }
        }
    }
}
