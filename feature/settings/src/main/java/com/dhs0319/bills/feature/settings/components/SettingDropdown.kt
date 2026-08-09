package com.dhs0319.bills.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <T> SettingDropdown(
    title: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val menuShape = RoundedCornerShape(24.dp)

    Card(modifier = modifier.fillMaxWidth()) {
        Box {
            ListItem(
                modifier = Modifier.clickable { expanded = true },
                headlineContent = {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(optionLabel(selected))
                },
                trailingContent = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "选择$title")
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(200.dp),
                shape = menuShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        leadingIcon = {
                            RadioButton(
                                selected = selected == option,
                                onClick = null
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
