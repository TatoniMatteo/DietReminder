package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R
import it.matato.dietreminder.common.model.FoodCategories
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.ui.theme.AppSpace
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

internal data class ShoppingEntry(val item: ShoppingListItem, val day: ShoppingListItemDay?) {
    val purchaseDay get() = day?.let { it.purchaseDayOfWeek ?: it.dayOfWeek } ?: (item.purchaseDayOfWeek ?: item.dayOfWeek)
    val bought get() = day?.isBought ?: item.isBought
    val neededDay get() = day?.dayOfWeek ?: item.dayOfWeek
    val key get() = "${item.id}:${day?.id ?: 0}"
}
internal fun shoppingEntries(list: ShoppingListWithItems) = list.items.flatMap { row ->
    if (row.days.isNotEmpty()) row.days.map { ShoppingEntry(row.item, it) } else listOf(ShoppingEntry(row.item, null))
}
private fun DayOfWeek.label() = getDisplayName(TextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.titlecase() }

@Composable
fun ShoppingDayPlanner(
    list: ShoppingListWithItems,
    onToggleItem: (ShoppingListItem) -> Unit,
    onToggleDay: (Long, Boolean) -> Unit,
    onEdit: (ShoppingListItem) -> Unit,
    onUpdate: (ShoppingListItem) -> Unit,
    onMove: (Long, Long?, DayOfWeek?) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onCompletedDelete: () -> Unit = {},
    onAllocation: (ShoppingListItemDay) -> Unit = {},
) {
    var selected by rememberSaveable(list.list.id) { mutableStateOf(-1) }
    var purchasedExpanded by rememberSaveable(list.list.id) { mutableStateOf(false) }
    var organizing by remember { mutableStateOf<ShoppingEntry?>(null) }
    val entries = remember(list) { shoppingEntries(list) }
    val disabled = LocalOfflineMode.current
    val purchased = entries.filter { it.bought }.sortedBy { it.item.name.lowercase() }
    val filtered = entries.filter { !it.bought && (selected == -1 || (it.purchaseDay?.value ?: 0) == selected) }
    if (organizing != null) {
        val entry = organizing!!
        androidx.activity.compose.BackHandler { organizing = null }
        ShoppingOrganizationForm(entry, onDismiss = { organizing = null }, onSave = { category, needed, purchase, amount, unit ->
            val updated = entry.item.copy(category = category, dayOfWeek = if (entry.day == null) needed else entry.item.dayOfWeek, purchaseDayOfWeek = if (entry.day == null) purchase else entry.item.purchaseDayOfWeek, amount = if (entry.day == null) amount else entry.item.amount, unit = if (entry.day == null) unit else entry.item.unit)
            if (updated != entry.item) onUpdate(updated)
            if (entry.day != null) {
                val updatedDay = entry.day.copy(purchaseDayOfWeek = purchase, amount = amount, unit = unit)
                if (updatedDay != entry.day) onAllocation(updatedDay)
            }
            organizing = null
        })
        return
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AppSpace.screen), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(-1 to "Tutti", 0 to "Senza giorno") + DayOfWeek.entries.map { it.value to it.label() }).forEach { (value, title) ->
                FilterChip(selected == value, { selected = value }, label = { Text(title) }, modifier = Modifier.testTag("shoppingDay_$value"))
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = AppSpace.screen, end = AppSpace.screen, top = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            if (filtered.isEmpty()) item { Text(stringResource(R.string.shopping_pending_empty), Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            filtered.groupBy { it.purchaseDay?.value ?: 0 }.toSortedMap().forEach { (day, dayEntries) ->
                item("day-$day") {
                    Text(if (day == 0) "Senza giorno · dispensa" else "Acquisti di ${DayOfWeek.of(day).label()}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                }
                dayEntries.groupBy { FoodCategories.classify(it.item.name, it.item.category) }.toSortedMap().forEach { (category, rows) ->
                    item("category-$day-$category") { Text("$category · ${rows.size} da acquistare", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
                    items(rows.sortedBy { it.item.name.lowercase() }, key = { it.key }) { entry ->
                        ShoppingEntryRow(entry, disabled, onToggleItem, onToggleDay, onEdit, onDelete,
                            onOrganize = { organizing = entry }, modifier = Modifier.animateItem())
                    }
                }
            }
            if (purchased.isNotEmpty()) {
                item("purchased-header") {
                    AppCard(Modifier.fillMaxWidth().clickable { purchasedExpanded = !purchasedExpanded }.testTag("shoppingPurchasedSection")) {
                        Row(Modifier.fillMaxWidth().padding(AppSpace.card), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.shopping_purchased, purchased.size), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Icon(if (purchasedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                stringResource(if (purchasedExpanded) R.string.collapse else R.string.expand))
                        }
                    }
                }
                if (purchasedExpanded) items(purchased, key = { it.key }) { entry ->
                    ShoppingEntryRow(entry, disabled, onToggleItem, onToggleDay, onEdit, onDelete,
                        onOrganize = { organizing = entry }, modifier = Modifier.animateItem())
                }
            }
            if (entries.isNotEmpty() && entries.all { it.bought }) item { CompletedListCard(onCompletedDelete) }
        }
    }

}

@Composable
private fun ShoppingEntryRow(
    entry: ShoppingEntry,
    disabled: Boolean,
    onToggleItem: (ShoppingListItem) -> Unit,
    onToggleDay: (Long, Boolean) -> Unit,
    onEdit: (ShoppingListItem) -> Unit,
    onDelete: (Long) -> Unit,
    onOrganize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(AppSpace.item), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(entry.bought, { if (entry.day != null) onToggleDay(entry.day.id, !entry.bought) else onToggleItem(entry.item) }, enabled = !disabled, modifier = Modifier.testTag("shoppingCheck_${entry.key}"))
            Column(Modifier.weight(1f).clickable(enabled = !disabled) { if (entry.day != null) onToggleDay(entry.day.id, !entry.bought) else onToggleItem(entry.item) }) {
                Text(entry.item.name, style = MaterialTheme.typography.titleMedium, textDecoration = if (entry.bought) TextDecoration.LineThrough else null, color = if (entry.bought) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                val quantity = entry.day?.displayQuantity ?: entry.item.displayQuantity
                if (quantity.isNotEmpty()) Text(quantity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                entry.neededDay?.let { Text("Serve ${it.label().lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton({ menu = true }, enabled = !disabled, modifier = Modifier.testTag("shoppingOptions_${entry.key}")) { Icon(Icons.Rounded.MoreVert, "Opzioni ${entry.item.name}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Giorno e categoria") }, { menu = false; onOrganize() })
                    DropdownMenuItem({ Text(stringResource(R.string.edit_item)) }, { menu = false; onEdit(entry.item) })
                    DropdownMenuItem({ Text("Rimuovi alimento dalla lista") }, { menu = false; onDelete(entry.item.id) })
                }
            }
        }
    }
}

@Composable
private fun ShoppingOrganizationForm(entry: ShoppingEntry, onDismiss: () -> Unit, onSave: (String?, DayOfWeek?, DayOfWeek?, String, it.matato.dietreminder.data.model.QuantityUnit) -> Unit) {
    var amount by remember { mutableStateOf(entry.day?.amount ?: entry.item.amount) }
    var unit by remember { mutableStateOf(entry.day?.unit ?: entry.item.unit) }
    var category by remember { mutableStateOf(entry.item.category) }
    var needed by remember { mutableStateOf(entry.neededDay) }
    var purchase by remember { mutableStateOf(entry.day?.purchaseDayOfWeek ?: entry.item.purchaseDayOfWeek) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppSpace.screen), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(entry.item.name, style = MaterialTheme.typography.headlineSmall)
            Text("Cambiare il giorno d’acquisto non sposta il pasto.", style = MaterialTheme.typography.bodySmall)
            it.matato.dietreminder.ui.components.QuantityInputRow(amount = amount, selectedUnit = unit, onAmountChange = { amount = it.take(200) }, onUnitChange = { unit = it })
            ChoiceField("Categoria", category ?: "Automatica · ${FoodCategories.classify(entry.item.name)}", listOf("Automatica" to null) + FoodCategories.all.map { it.name to it.name }) { category = it }
            OutlinedTextField(category ?: "", { category = it.take(80).takeIf(String::isNotBlank) }, label = { Text("Categoria personalizzata") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (entry.day == null) ChoiceField("Giorno di utilizzo", needed?.label() ?: "Non assegnato", listOf("Non assegnato" to null) + DayOfWeek.entries.map { it.label() to it }) { needed = it }
            else Text("Serve ${needed?.label()}", style = MaterialTheme.typography.bodyMedium)
            ChoiceField("Giorno d’acquisto", purchase?.label() ?: "Come il giorno di utilizzo", listOf("Come il giorno di utilizzo" to null) + DayOfWeek.entries.map { it.label() to it }) { purchase = it }
            Button({ onSave(category?.trim()?.takeIf(String::isNotBlank), needed, purchase, amount, unit) }) { Text("Salva") }
            TextButton(onDismiss) { Text("Annulla") }
    }
}

@Composable
internal fun <T> ChoiceField(label: String, selected: String, options: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text(selected) }
            DropdownMenu(open, { open = false }) { options.forEach { (title, value) -> DropdownMenuItem({ Text(title) }, { open = false; onSelect(value) }, modifier = Modifier.testTag("shoppingChoice_${label}_$title")) } }
        }
    }
}
