package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DietsActionsMenu(
    expanded: Boolean,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onConfigure: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (!isActive) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.activate))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                    )
                },
                onClick = onActivate,
            )
        }

        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.configure))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                )
            },
            onClick = onConfigure,
        )

        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.duplicate))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                )
            },
            onClick = onDuplicate,
        )

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
            onClick = onDelete,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DietsActionsMenuPreview() {
    DietTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DietsActionsMenu(
                expanded = true,
                isActive = false,
                onDismiss = {},
                onActivate = {},
                onConfigure = {},
                onDuplicate = {},
                onDelete = {},
            )
        }
    }
}