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
fun OfflineScreen(
	onRetryClick: () -> Unit,
	onContinueClick: () -> Unit,
) {
    AppStatusPage(
        icon = Icons.Rounded.CloudOff, title = stringResource(R.string.offline_title), message = stringResource(R.string.offline_message),
        primaryLabel = stringResource(R.string.retry), onPrimary = onRetryClick,
        secondaryLabel = stringResource(R.string.continue_offline), onSecondary = onContinueClick,
    )
}
