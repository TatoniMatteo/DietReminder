package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun MealNotesSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(
            title = stringResource(R.string.additional_notes),
            icon = Icons.AutoMirrored.Rounded.StickyNote2,
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            minLines = 3,
        )
    }
}

@Composable
fun CustomLabelField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            androidx.compose.material3.Text(stringResource(R.string.custom_label))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun MealNotesSectionPreview() {
    DietTheme {
        MealNotesSection(
            description = "Some notes about the meal",
            onDescriptionChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomLabelFieldPreview() {
    DietTheme {
        CustomLabelField(
            value = "Custom Label",
            onValueChange = {},
        )
    }
}
