package it.matato.dietreminder.ui.screens.shoppinglist

import android.Manifest
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun ShoppingReminderControls(list: ShoppingList, onChange: (Boolean, Int) -> Unit) {
    val context = LocalContext.current
    val offline = LocalOfflineMode.current
    fun blocked(): Boolean = !NotificationManagerCompat.from(context).areNotificationsEnabled() || context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel(it.matato.dietreminder.util.shopping.ShoppingReminderWorker.CHANNEL)?.importance == android.app.NotificationManager.IMPORTANCE_NONE
    var denied by remember { mutableStateOf(blocked()) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { denied = blocked() }
    var hour by remember(list.reminderTimeMinutes) { mutableStateOf("%02d:%02d".format(list.reminderTimeMinutes / 60, list.reminderTimeMinutes % 60)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) onChange(true, list.reminderTimeMinutes)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.screen, vertical = AppSpace.item)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ricordami la spesa", style = MaterialTheme.typography.titleSmall)
                Text("Il giorno prima, solo per gli alimenti mancanti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(list.remindersEnabled, { enabled ->
                if (!enabled) onChange(false, list.reminderTimeMinutes)
                else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { denied = blocked(); onChange(true, list.reminderTimeMinutes) }
            }, enabled = !offline, modifier = Modifier.testTag("shoppingReminders"))
        }
        if (list.remindersEnabled) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
                OutlinedTextField(hour, { hour = it.take(5) }, label = { Text("Verso le (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !offline)
                val parts = hour.split(':').map { it.toIntOrNull() }
                val valid = parts.size == 2 && parts[0] in 0..23 && parts[1] in 0..59
                TextButton({ onChange(true, parts[0]!! * 60 + parts[1]!!) }, enabled = valid && !offline) { Text("Salva orario") }
            }
        }
        if (denied || (list.remindersEnabled && blocked())) {
            Text("Le notifiche sono bloccate nelle impostazioni del telefono.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            TextButton({ settingsLauncher.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Apri impostazioni notifiche") }
        }
    }
}
