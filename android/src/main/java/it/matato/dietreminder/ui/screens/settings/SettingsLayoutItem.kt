package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.theme.LocalCompactLayout
import it.matato.dietreminder.ui.theme.UiLayoutPreferences

@Composable
fun SettingsLayoutItem() {
    val context = LocalContext.current
    val preferences = remember(context) { UiLayoutPreferences(context) }
    val compact = LocalCompactLayout.current
    Column(Modifier.fillMaxWidth()) {
        SettingsListItem(
            title = stringResource(R.string.layout_density),
            subtitle = stringResource(if (compact) R.string.layout_compact_description else R.string.layout_standard_description),
            leadingIcon = Icons.Rounded.ViewAgenda,
            onClick = { preferences.setCompact(!compact) },
        )
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = AppSpace.card).padding(bottom = AppSpace.group)
        ) {
            listOf(
                false to R.string.layout_standard,
                true to R.string.layout_compact
            ).forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = compact == value, onClick = { preferences.setCompact(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(if (value) "layoutCompact" else "layoutStandard"),
                ) { Text(stringResource(label)) }
            }
        }
    }
}
