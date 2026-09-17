package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek

@Composable
fun MealDetailScreen(
    vm: DietViewModel, mealId: Long, dietId: Long, dayOfWeek: DayOfWeek, onBack: () -> Unit
) {
    var mealEntity by remember { mutableStateOf<Meal?>(null) }
    var courses by remember { mutableStateOf<List<CourseWithItems>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var type by remember { mutableStateOf(MealType.LUNCH) }
    var customLabel by remember { mutableStateOf("") }

    var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var timePickerVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mealId) {
        if (mealId != 0L) {
            val details = vm.getMeal(mealId)

            details?.let {
                mealEntity = it.meal
                courses = it.courses
                description = it.meal.description
                time = "%02d:%02d".format(
                    it.meal.timeMinutes / 60, it.meal.timeMinutes % 60
                )
                type = it.meal.type
                customLabel = it.meal.customTypeLabel ?: ""
            }
        } else {
            val defaultTimeMinutes = vm.getDefaultTime(MealType.LUNCH)

            time = "%02d:%02d".format(
                defaultTimeMinutes / 60, defaultTimeMinutes % 60
            )

            mealEntity = Meal(
                dietId = dietId, dayOfWeek = dayOfWeek, type = MealType.LUNCH, timeMinutes = defaultTimeMinutes
            )
        }

        isLoading = false
    }

    fun addCourse() {
        val meal = mealEntity ?: return

        courses = courses + CourseWithItems(
            course = Course(
                mealId = meal.id, name = "", order = courses.size
            ), items = emptyList()
        )
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 20.dp, bottom = 104.dp
                )
            ) {
                item {
                    MealHeader(mealId = mealId, type = type, time = time, onBack = onBack, onSave = {
                        mealEntity?.let {
                            val parts = time.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull()
                            val minute = parts.getOrNull(1)?.toIntOrNull()

                            if (hour != null && minute != null) {
                                val updated = it.copy(
                                    description = description.trim(),
                                    timeMinutes = hour * 60 + minute,
                                    type = type,
                                    customTypeLabel = if (type == MealType.OTHER) {
                                        customLabel.trim().ifBlank { null }
                                    } else {
                                        null
                                    })

                                vm.saveMeal(updated, courses)
                                onBack()
                            }
                        }
                    }, onDelete = {
                        if (mealId != 0L) {
                            vm.deleteMeal(mealId)
                            onBack()
                        }
                    })
                }

                item {
                    MealInformationSection(time = time, onTimeClick = {
                        timePickerVisible = true
                    }, type = type, typeMenuExpanded = typeMenuExpanded, onTypeMenuExpandedChange = {
                        typeMenuExpanded = it
                    }, onTypeChange = { newType ->
                        type = newType

                        val newDefaultTime = vm.getDefaultTime(newType)

                        time = "%02d:%02d".format(
                            newDefaultTime / 60, newDefaultTime % 60
                        )
                    })
                }

                if (type == MealType.OTHER) {
                    item {
                        OutlinedTextField(
                            value = customLabel, onValueChange = {
                                customLabel = it
                            }, label = {
                                Text(stringResource(R.string.custom_label))
                            }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, singleLine = true
                        )
                    }
                }

                item {
                    MealNotesSection(
                        description = description, onDescriptionChange = {
                            description = it
                        })
                }

                item {
                    CoursesHeader(
                        count = courses.size
                    )
                }

                if (courses.isEmpty()) {
                    item {
                        EmptyCourses(
                            onAddCourse = {
                                addCourse()
                            })
                    }
                } else {
                    item {
                        CoursesList(courses = courses, onUpdate = { index, updated ->
                            courses = courses.toMutableList().apply {
                                set(index, updated)
                            }
                        }, onDelete = { index ->
                            courses = courses.toMutableList().apply {
                                removeAt(index)
                            }
                        }, onAddCourse = {
                            addCourse()
                        })
                    }
                }
            }
        }
    }

    if (timePickerVisible) {
        TimePickerDialog(time = time, onDismiss = {
            timePickerVisible = false
        }, onConfirm = { selectedTime ->
            time = selectedTime
            timePickerVisible = false
        })
    }
}

@Composable
private fun MealHeader(
    mealId: Long, type: MealType, time: String, onBack: () -> Unit, onSave: () -> Unit, onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (mealId == 0L) {
                    stringResource(R.string.new_meal)
                } else {
                    stringResource(R.string.edit_meal)
                }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
            )

            Text(
                text = "${stringResource(type.resId)} · $time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mealId != 0L) {
            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Button(
            onClick = onSave
        ) {
            Icon(
                imageVector = Icons.Rounded.Save, contentDescription = null
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun MealInformationSection(
    time: String,
    onTimeClick: () -> Unit,
    type: MealType,
    typeMenuExpanded: Boolean,
    onTypeMenuExpandedChange: (Boolean) -> Unit,
    onTypeChange: (MealType) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
    ) {
        Column {
            ListItem(headlineContent = {
                Text(
                    text = stringResource(R.string.time_label), fontWeight = FontWeight.SemiBold
                )
            }, supportingContent = {
                Text(
                    text = stringResource(R.string.meal_time), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }, leadingContent = {
                IconContainer(
                    icon = Icons.Rounded.Schedule
                )
            }, trailingContent = {
                OutlinedButton(
                    onClick = onTimeClick
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule, contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = time, style = MaterialTheme.typography.labelLarge
                    )
                }
            })

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box {
                ListItem(modifier = Modifier.clickable {
                    onTypeMenuExpandedChange(true)
                }, headlineContent = {
                    Text(
                        text = stringResource(R.string.meal_type), fontWeight = FontWeight.SemiBold
                    )
                }, supportingContent = {
                    Text(
                        text = stringResource(type.resId), color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }, leadingContent = {
                    IconContainer(
                        icon = Icons.Rounded.Restaurant
                    )
                })

                DropdownMenu(
                    expanded = typeMenuExpanded, onDismissRequest = {
                        onTypeMenuExpandedChange(false)
                    }) {
                    MealType.entries.forEach { mealType ->
                        DropdownMenuItem(text = {
                            Text(stringResource(mealType.resId))
                        }, onClick = {
                            onTypeChange(mealType)
                            onTypeMenuExpandedChange(false)
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    time: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    val parts = time.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour, initialMinute = initialMinute, is24Hour = true
    )

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(stringResource(R.string.time_label))
    }, text = {
        TimePicker(
            state = timePickerState
        )
    }, confirmButton = {
        TextButton(
            onClick = {
                onConfirm(
                    "%02d:%02d".format(
                        timePickerState.hour, timePickerState.minute
                    )
                )
            }) {
            Text(stringResource(R.string.save))
        }
    }, dismissButton = {
        TextButton(
            onClick = onDismiss
        ) {
            Text(stringResource(R.string.back))
        }
    })
}

@Composable
private fun MealNotesSection(
    description: String, onDescriptionChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.additional_notes), icon = Icons.AutoMirrored.Rounded.StickyNote2
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            minLines = 3
        )
    }
}

@Composable
private fun CoursesHeader(
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            title = stringResource(R.string.courses), icon = Icons.Rounded.Restaurant, modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = 10.dp, vertical = 5.dp
                )
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CoursesList(
    courses: List<CourseWithItems>, onUpdate: (Int, CourseWithItems) -> Unit, onDelete: (Int) -> Unit, onAddCourse: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
    ) {
        Column {
            courses.forEachIndexed { index, course ->
                CourseListItem(index = index, courseWithItems = course, onUpdate = {
                    onUpdate(index, it)
                }, onDelete = {
                    onDelete(index)
                })

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            TextButton(
                onClick = onAddCourse, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add, contentDescription = null
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(stringResource(R.string.add_course))
            }
        }
    }
}

@Composable
private fun CourseListItem(
    index: Int, courseWithItems: CourseWithItems, onUpdate: (CourseWithItems) -> Unit, onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp, vertical = 14.dp
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = courseWithItems.course.name, onValueChange = {
                    onUpdate(
                        courseWithItems.copy(
                            course = courseWithItems.course.copy(
                                name = it
                            )
                        )
                    )
                }, modifier = Modifier.weight(1f), label = {
                    Text(stringResource(R.string.course_name_hint))
                }, shape = MaterialTheme.shapes.medium, singleLine = true
            )

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        courseWithItems.items.forEachIndexed { itemIndex, item ->
            FoodItemRow(item = item, onUpdate = { updatedItem ->
                val updatedItems = courseWithItems.items.toMutableList().apply {
                    set(itemIndex, updatedItem)
                }

                onUpdate(
                    courseWithItems.copy(
                        items = updatedItems
                    )
                )
            }, onDelete = {
                val updatedItems = courseWithItems.items.toMutableList().apply {
                    removeAt(itemIndex)
                }

                onUpdate(
                    courseWithItems.copy(
                        items = updatedItems
                    )
                )
            })

            if (itemIndex < courseWithItems.items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = 16.dp, top = 4.dp, bottom = 4.dp
                    )
                )
            }
        }

        TextButton(
            onClick = {
                val newItem = FoodItem(
                    courseId = courseWithItems.course.id, name = "", quantities = emptyList(), order = courseWithItems.items.size
                )

                onUpdate(
                    courseWithItems.copy(
                        items = courseWithItems.items + newItem
                    )
                )
            }, modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add, contentDescription = null
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(stringResource(R.string.add_food_item))
        }
    }
}

@Composable
private fun FoodItemRow(
    item: FoodItem, onUpdate: (FoodItem) -> Unit, onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = item.name, onValueChange = {
                onUpdate(item.copy(name = it))
            }, modifier = Modifier.fillMaxWidth(), label = {
                Text(stringResource(R.string.food_item))
            }, shape = MaterialTheme.shapes.medium, singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = item.quantities.joinToString(", "), onValueChange = { value ->
                    onUpdate(item.copy(quantities = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
                }, modifier = Modifier.weight(1f), label = {
                    Text(stringResource(R.string.quantity))
                }, shape = MaterialTheme.shapes.medium, singleLine = true
            )

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyCourses(
    onAddCourse: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddCourse), shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconContainer(
                icon = Icons.Rounded.Restaurant, size = 56.dp, iconSize = 28.dp
            )

            Text(
                text = stringResource(R.string.courses),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.add_course),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String, icon: ImageVector, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconContainer(
    icon: ImageVector, size: Dp = 40.dp, iconSize: Dp = 21.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize)
        )
    }
}
