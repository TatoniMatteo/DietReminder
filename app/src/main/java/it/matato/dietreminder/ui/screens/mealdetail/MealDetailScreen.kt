package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.CourseEntity
import it.matato.dietreminder.data.CourseWithItems
import it.matato.dietreminder.data.FoodItemEntity
import it.matato.dietreminder.data.MealEntity
import it.matato.dietreminder.data.MealType
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    vm: DietViewModel,
    mealId: Long,
    dietId: Long,
    dayOfWeek: DayOfWeek,
    onBack: () -> Unit
) {
    var mealEntity by remember { mutableStateOf<MealEntity?>(null) }
    var courses by remember { mutableStateOf<List<CourseWithItems>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var type by remember { mutableStateOf(MealType.LUNCH) }
    var customLabel by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }

    LaunchedEffect(mealId) {
        if (mealId != 0L) {
            val details = vm.getMeal(mealId)
            details?.let {
                mealEntity = it.meal
                courses = it.courses
                description = it.meal.description
                time = "%02d:%02d".format(it.meal.timeMinutes / 60, it.meal.timeMinutes % 60)
                type = it.meal.type
                customLabel = it.meal.customTypeLabel ?: ""
            }
        } else {
            val defaultTimeMinutes = vm.getDefaultTime(MealType.LUNCH)
            time = "%02d:%02d".format(defaultTimeMinutes / 60, defaultTimeMinutes % 60)
            mealEntity = MealEntity(dietId = dietId, dayOfWeek = dayOfWeek, type = MealType.LUNCH, timeMinutes = defaultTimeMinutes)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealId == 0L) stringResource(R.string.new_meal) else stringResource(R.string.edit_meal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (mealId != 0L) {
                        IconButton(onClick = {
                            vm.deleteMeal(mealId)
                            onBack()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            mealEntity?.let {
                                val updated = it.copy(
                                    description = description.trim(),
                                    timeMinutes = parseTime(time) ?: it.timeMinutes,
                                    type = type,
                                    customTypeLabel = if (type == MealType.OTHER) customLabel.trim().ifBlank { null } else null
                                )
                                vm.saveMeal(updated, courses)
                                onBack()
                            }
                        },
                        enabled = parseTime(time) != null,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it.take(5) },
                            label = { Text(stringResource(R.string.time_label)) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedType = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(stringResource(type.resId))
                            }
                            DropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                MealType.entries.forEach { mealType ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(mealType.resId)) },
                                        onClick = {
                                            type = mealType
                                            val newDefaultTime = vm.getDefaultTime(mealType)
                                            time = "%02d:%02d".format(newDefaultTime / 60, newDefaultTime % 60)
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (type == MealType.OTHER) {
                    item {
                        OutlinedTextField(
                            value = customLabel,
                            onValueChange = { customLabel = it },
                            label = { Text(stringResource(R.string.custom_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.additional_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        minLines = 2
                    )
                }

                item {
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.courses), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            courses = courses + CourseWithItems(
                                course = CourseEntity(mealId = mealId, name = "Nuova portata", order = courses.size),
                                items = emptyList()
                            )
                        }) {
                            Icon(Icons.Rounded.Add, null)
                            Text(stringResource(R.string.add_course))
                        }
                    }
                }

                itemsIndexed(courses) { cIndex, courseWithItems ->
                    CourseEditorCard(
                        courseWithItems = courseWithItems,
                        onUpdate = { updated ->
                            courses = courses.toMutableList().apply { set(cIndex, updated) }
                        },
                        onDelete = {
                            courses = courses.toMutableList().apply { removeAt(cIndex) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseEditorCard(
    courseWithItems: CourseWithItems,
    onUpdate: (CourseWithItems) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = courseWithItems.course.name,
                    onValueChange = { onUpdate(courseWithItems.copy(course = courseWithItems.course.copy(name = it))) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.course_name_hint)) },
                    shape = MaterialTheme.shapes.medium
                )
                IconButton(onClick = onDelete, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }

            courseWithItems.items.forEachIndexed { iIndex, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { name ->
                            val updatedItems = courseWithItems.items.toMutableList().apply {
                                set(iIndex, item.copy(name = name))
                            }
                            onUpdate(courseWithItems.copy(items = updatedItems))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.food_item)) },
                        shape = MaterialTheme.shapes.medium,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = item.quantities.joinToString(", "),
                        onValueChange = { qStr ->
                            val updatedItems = courseWithItems.items.toMutableList().apply {
                                set(iIndex, item.copy(quantities = qStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
                            }
                            onUpdate(courseWithItems.copy(items = updatedItems))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.quantity)) },
                        shape = MaterialTheme.shapes.medium,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(onClick = {
                        val updatedItems = courseWithItems.items.toMutableList().apply { removeAt(iIndex) }
                        onUpdate(courseWithItems.copy(items = updatedItems))
                    }) {
                        Icon(Icons.Rounded.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            TextButton(
                onClick = {
                    val newItem = FoodItemEntity(
                        courseId = courseWithItems.course.id,
                        name = "",
                        quantities = emptyList(),
                        order = courseWithItems.items.size
                    )
                    onUpdate(courseWithItems.copy(items = courseWithItems.items + newItem))
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Rounded.Add, null)
                Text(stringResource(R.string.add_food_item))
            }
        }
    }
}

private fun parseTime(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null
    if (hour !in 0 .. 23 || minute !in 0 .. 59) return null
    return hour * 60 + minute
}
