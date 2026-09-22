package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun HydrationRangeItem(
    range: HydrationRange,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(
        range.startMinutes,
        range.endMinutes,
    ) {
        mutableStateOf(false)
    }

    ListItem(
        headlineContent = {
            Text(
                text = formatRange(range),
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.hydration_windows),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            IconContainer(
                icon = Icons.Rounded.Schedule,
            )
        },
        trailingContent = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.delete),
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
    )
}

private fun formatRange(range: HydrationRange): String {
    return "%02d:%02d – %02d:%02d".format(
        range.startMinutes / 60,
        range.startMinutes % 60,
        range.endMinutes / 60,
        range.endMinutes % 60,
    )
}

@Preview(showBackground = true)
@Composable
private fun HydrationRangeItemPreview() {
    DietTheme {
        HydrationRangeItem(
            range = HydrationRange(540, 1080),
            onDelete = {},
        )
    }
}