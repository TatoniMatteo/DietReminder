package it.matato.dietreminder.ui.screens.update

import it.matato.dietreminder.ui.components.AppStatusPage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R

@Composable
fun ObsoleteScreen(
	onUpdateClick: () -> Unit,
) {
    AppStatusPage(
        icon = Icons.Rounded.WarningAmber, title = stringResource(R.string.update_obsolete_title), message = stringResource(R.string.update_obsolete_message),
        primaryLabel = stringResource(R.string.update_now), onPrimary = onUpdateClick,
    )
}
