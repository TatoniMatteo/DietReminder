package it.matato.dietreminder.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.select_language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    ),
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.lang_it))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onLanguageSelected("it")
                    },
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.lang_en))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onLanguageSelected("en")
                    },
                )

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguagePickerDialogPreview() {
    DietTheme {
        LanguagePickerDialog(
            onDismiss = {},
            onLanguageSelected = {},
        )
    }
}
