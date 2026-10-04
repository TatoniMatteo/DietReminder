package it.matato.dietreminder.ui

import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

val LocalOfflineMode = compositionLocalOf { false }

@Composable
fun OfflineAwareFloatingActionButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	containerColor: Color,
	contentColor: Color,
	content: @Composable () -> Unit,
) {
	val isOffline = LocalOfflineMode.current
	FloatingActionButton(
		onClick = { if (!isOffline) onClick() },
		modifier = modifier.semantics {
			if (isOffline) disabled()
		},
		containerColor = containerColor,
		contentColor = contentColor,
		content = content,
	)
}
