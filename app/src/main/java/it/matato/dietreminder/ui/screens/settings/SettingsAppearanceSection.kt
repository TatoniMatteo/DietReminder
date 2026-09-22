package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun SettingsAppearanceSection(
    currentTheme: String,
    dynamicEnabled: Boolean,
    seedColorHex: String,
    currentLanguage: String,
    onThemeChange: (String) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onColorClick: () -> Unit,
    onLanguageClick: () -> Unit,
) {
    val seedColor = parseSeedColor(seedColorHex)

    SettingsSection(
        title = stringResource(R.string.appearance_personalization),
        icon = Icons.Rounded.Tune,
    ) {
        SettingsListItem(
            title = stringResource(R.string.theme),
            subtitle = when (currentTheme) {
                "light" -> stringResource(R.string.theme_light)
                "dark" -> stringResource(R.string.theme_dark)
                else -> stringResource(R.string.theme_system)
            },
            leadingIcon = Icons.Rounded.Brightness4,
            onClick = {
                val nextTheme = when (currentTheme) {
                    "system" -> "light"
                    "light" -> "dark"
                    else -> "system"
                }

                onThemeChange(nextTheme)
            },
        )

        SettingsDivider()

        SettingsListItem(
            title = stringResource(R.string.dynamic_colors),
            subtitle = stringResource(R.string.dynamic_colors_desc),
            leadingIcon = Icons.Rounded.ColorLens,
            trailingContent = {
                Switch(
                    checked = dynamicEnabled,
                    onCheckedChange = onDynamicColorsChange,
                )
            },
            onClick = {
                onDynamicColorsChange(!dynamicEnabled)
            },
        )

        SettingsDivider()

        SettingsListItem(
            title = stringResource(R.string.app_color),
            subtitle = stringResource(R.string.app_color_desc),
            leadingIcon = Icons.Rounded.ColorLens,
            trailingContent = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = seedColor,
                            shape = CircleShape,
                        ),
                )
            },
            onClick = onColorClick,
        )

        SettingsDivider()

        SettingsListItem(
            title = stringResource(R.string.language),
            subtitle = if (currentLanguage == "it") {
                stringResource(R.string.lang_it)
            } else {
                stringResource(R.string.lang_en)
            },
            leadingIcon = Icons.Rounded.Language,
            onClick = onLanguageClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAppearanceSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsAppearanceSection(
                currentTheme = "system",
                dynamicEnabled = true,
                seedColorHex = "0xFF6750A4",
                currentLanguage = "en",
                onThemeChange = {},
                onDynamicColorsChange = {},
                onColorClick = {},
                onLanguageClick = {},
            )
        }
    }
}