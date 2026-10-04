package it.matato.dietreminder.ui.screens.importdiet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.getLocalizedImportError
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.io.InputStreamReader
import java.time.format.TextStyle
import kotlinx.coroutines.launch

@Composable
fun ImportDietScreen(
	vm: DietViewModel,
	onBack: () -> Unit,
	onImportSuccess: () -> Unit,
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	var tabIndex by remember { mutableIntStateOf(0) }
	var jsonText by remember { mutableStateOf("") }
	var parsedDiet by remember { mutableStateOf<DietExport?>(null) }
	var isConflict by remember { mutableStateOf(value = false) }
	var errorMessage by remember { mutableStateOf<String?>(null) }

	fun processJson(rawJson: String) {
		scope.launch {
			try {
				val export = vm.parseDietJson(rawJson)
				parsedDiet = export
				jsonText = rawJson
				isConflict = export.uuid?.let { uuid -> vm.dietExists(uuid) } ?: false
				errorMessage = null
			} catch (t: Throwable) {
				parsedDiet = null
				errorMessage = t.getLocalizedImportError(context)
			}
		}
	}

	val openDocumentLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		uri?.let { selectedUri ->
			try {
				context.contentResolver.openInputStream(selectedUri)?.use { input ->
					val text = InputStreamReader(input).use { it.readText() }
					processJson(text)
				}
			} catch (t: Throwable) {
				parsedDiet = null
				errorMessage = t.getLocalizedImportError(context)
			}
		}
	}

	ImportDietContent(
		tabIndex = tabIndex,
		jsonText = jsonText,
		parsedDiet = parsedDiet,
		isConflict = isConflict,
		errorMessage = errorMessage,
		onTabSelected = {
			tabIndex = it
			parsedDiet = null
			errorMessage = null
		},
		onJsonTextChange = { jsonText = it },
		onAnalyzeTextClick = { processJson(jsonText) },
		onPickFileClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
		onConfirmImport = {
			scope.launch {
				parsedDiet?.let { diet ->
					vm.importDiet(jsonText, overwrite = isConflict).onSuccess {
						onImportSuccess()
					}.onFailure { t ->
						errorMessage = t.getLocalizedImportError(context)
					}
				}
			}
		},
		onDismissError = { errorMessage = null },
		onBack = onBack,
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDietContent(
	tabIndex: Int,
	jsonText: String,
	parsedDiet: DietExport?,
	isConflict: Boolean,
	errorMessage: String?,
	onTabSelected: (Int) -> Unit,
	onJsonTextChange: (String) -> Unit,
	onAnalyzeTextClick: () -> Unit,
	onPickFileClick: () -> Unit,
	onConfirmImport: () -> Unit,
	onDismissError: () -> Unit,
	onBack: () -> Unit,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = stringResource(R.string.import_diet),
						fontWeight = FontWeight.Bold,
					)
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
			)
		},
	) { innerPadding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
			contentPadding = PaddingValues(
				start = 16.dp,
				end = 16.dp,
				top = 12.dp,
				bottom = 32.dp,
			),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			item {
				SecondaryTabRow(selectedTabIndex = tabIndex) {
					Tab(
						selected = tabIndex == 0,
						onClick = { onTabSelected(0) },
						text = { Text(stringResource(R.string.import_text)) },
					)
					Tab(
						selected = tabIndex == 1,
						onClick = { onTabSelected(1) },
						text = { Text(stringResource(R.string.import_file)) },
					)
				}
			}

			if (tabIndex == 0) {
				item {
					Column(
						verticalArrangement = Arrangement.spacedBy(12.dp),
					) {
						OutlinedTextField(
							value = jsonText,
							onValueChange = onJsonTextChange,
							label = { Text(stringResource(R.string.json_content)) },
							modifier = Modifier
								.fillMaxWidth()
								.height(180.dp),
							shape = MaterialTheme.shapes.medium,
						)

						Button(
							onClick = onAnalyzeTextClick,
							enabled = jsonText.isNotBlank(),
							modifier = Modifier.fillMaxWidth(),
						) {
							Icon(Icons.Rounded.Preview, contentDescription = null)
							Spacer(Modifier.width(8.dp))
							Text(
								text = "Mostra anteprima",
								fontWeight = FontWeight.Bold,
							)
						}
					}
				}
			} else {
				item {
					OutlinedCard(
						modifier = Modifier.fillMaxWidth(),
						shape = MaterialTheme.shapes.large,
					) {
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.padding(24.dp),
							contentAlignment = Alignment.Center,
						) {
							Button(onClick = onPickFileClick) {
								Icon(
									imageVector = Icons.Rounded.FileOpen,
									contentDescription = null,
								)
								Spacer(Modifier.width(8.dp))
								Text(
									text = stringResource(R.string.import_file),
									fontWeight = FontWeight.Bold,
								)
							}
						}
					}
				}
			}

			if (parsedDiet != null) {
				item {
					DietPreviewCard(
						diet = parsedDiet,
						isConflict = isConflict,
						onConfirmImport = onConfirmImport,
					)
				}
			}
		}
	}

	errorMessage?.let { msg ->
		ImportErrorRedDialog(
			errorMessage = msg,
			onDismiss = onDismissError,
		)
	}
}

@Composable
private fun DietPreviewCard(
	diet: DietExport,
	isConflict: Boolean,
	onConfirmImport: () -> Unit,
) {
	ElevatedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.extraLarge,
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = diet.name,
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.weight(1f),
				)

				Box(
					modifier = Modifier
						.clip(MaterialTheme.shapes.small)
						.background(MaterialTheme.colorScheme.primaryContainer)
						.padding(horizontal = 8.dp, vertical = 4.dp),
				) {
					Text(
						text = "Anteprima",
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
					)
				}
			}

			Text(
				text = "Finestra prossimo pasto: ${diet.nextMealWindowMinutes} min • ${diet.meals.size} pasti totali",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			diet.meals.forEach { meal ->
				val dayName = meal.day.getDisplayName(
					TextStyle.FULL,
					LocalLocale.current.platformLocale,
				).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

				Column(
					verticalArrangement = Arrangement.spacedBy(4.dp),
				) {
					Text(
						text = "• $dayName - ${meal.type.name} (${meal.time})",
						style = MaterialTheme.typography.titleSmall,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary,
					)

					meal.courses.forEach { course ->
						course.items.forEach { item ->
							Text(
								text = "   - ${item.name} (${item.displayQuantity.ifBlank { "Nessuna quantità" }})",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurface,
							)
						}
					}
				}
			}

			if (isConflict) {
				OutlinedCard(
					modifier = Modifier.fillMaxWidth(),
					shape = MaterialTheme.shapes.medium,
				) {
					Row(
						modifier = Modifier.padding(12.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Icon(
							imageVector = Icons.Rounded.Info,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.error,
						)
						Spacer(Modifier.width(8.dp))
						Text(
							text = "Una dieta con questo identificatore esiste già. Procedendo verrà sovrascritta.",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.error,
						)
					}
				}
			}

			Button(
				onClick = onConfirmImport,
				modifier = Modifier.fillMaxWidth(),
			) {
				Icon(Icons.Rounded.CheckCircle, contentDescription = null)
				Spacer(Modifier.width(8.dp))
				Text(
					text = if (isConflict) "Sovrascrivi e Salva Dieta" else "Conferma e Salva Dieta",
					fontWeight = FontWeight.Bold,
				)
			}
		}
	}
}

@Composable
private fun ImportErrorRedDialog(
	errorMessage: String,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		icon = {
			Icon(
				imageVector = Icons.Rounded.ErrorOutline,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.error,
				modifier = Modifier.size(40.dp),
			)
		},
		title = {
			Text(
				text = stringResource(R.string.import_error_title),
				color = MaterialTheme.colorScheme.error,
				fontWeight = FontWeight.Bold,
			)
		},
		text = {
			Text(
				text = errorMessage,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurface,
			)
		},
		confirmButton = {
			Button(
				onClick = onDismiss,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.errorContainer,
					contentColor = MaterialTheme.colorScheme.onErrorContainer,
				),
			) {
				Text(
					text = "Capito",
					fontWeight = FontWeight.Bold,
				)
			}
		},
	)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ImportDietContentPreview() {
	DietTheme {
		ImportDietContent(
			tabIndex = 0,
			jsonText = "",
			parsedDiet = null,
			isConflict = false,
			errorMessage = null,
			onTabSelected = {},
			onJsonTextChange = {},
			onAnalyzeTextClick = {},
			onPickFileClick = {},
			onConfirmImport = {},
			onDismissError = {},
		) {}
	}
}
