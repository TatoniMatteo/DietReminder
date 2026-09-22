package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun MealHeader(
    isNew: Boolean,
    type: MealType,
    timeMinutes: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = stringResource(
                    if (isNew) R.string.new_meal else R.string.edit_meal,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "${stringResource(type.resId)} · ${formatTime(timeMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!isNew) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Button(onClick = onSave) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(stringResource(R.string.save))
        }
    }
}

private fun formatTime(timeMinutes: Int): String {
    return "%02d:%02d".format(
        timeMinutes / 60,
        timeMinutes % 60,
    )
}

@Preview(showBackground = true)
@Composable
private fun MealHeaderNewPreview() {
    DietTheme {
        MealHeader(
            isNew = true,
            type = MealType.BREAKFAST,
            timeMinutes = 480,
            onBack = {},
            onSave = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealHeaderEditPreview() {
    DietTheme {
        MealHeader(
            isNew = false,
            type = MealType.LUNCH,
            timeMinutes = 780,
            onBack = {},
            onSave = {},
            onDelete = {},
        )
    }
}
