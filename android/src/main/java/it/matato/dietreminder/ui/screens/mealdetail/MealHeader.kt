package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.resId
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun MealActions(
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offline = LocalOfflineMode.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.inline)
    ) {
        FilledTonalIconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
        }
        Spacer(Modifier.weight(1f))
        if (!isNew) {
            IconButton(onClick = onDelete, enabled = !offline) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Button(
            onClick = onSave,
            enabled = !offline,
            contentPadding = PaddingValues(horizontal = AppSpace.screen),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpace.inline))
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
fun MealHeader(
    isNew: Boolean,
    type: MealType,
    timeMinutes: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.group)) {
        if (showActions) MealActions(isNew, onBack, onSave, onDelete)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
            Text(
                stringResource(if (isNew) R.string.new_meal else R.string.edit_meal),
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold
            )
            Text(
                "${stringResource(type.resId)} · %02d:%02d".format(timeMinutes / 60, timeMinutes % 60),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
