package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.dialog.TimePickerDialog
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek

@Composable
fun MealDetailScreen(
    vm: DietViewModel,
    mealId: Long,
    dietId: Long,
    dayOfWeek: DayOfWeek,
    onBack: () -> Unit,
) {
    var meal by remember { mutableStateOf<Meal?>(null) }
    var courses by remember { mutableStateOf<List<CourseWithItems>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var description by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableIntStateOf(0) }
    var type by remember { mutableStateOf(MealType.LUNCH) }
    var customLabel by remember { mutableStateOf("") }

    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mealId, dietId, dayOfWeek) {
        val mealWithDetails = vm.getMeal(mealId)

        if (mealWithDetails != null) {
            meal = mealWithDetails.meal
            courses = mealWithDetails.courses
            description = mealWithDetails.meal.description
            timeMinutes = mealWithDetails.meal.timeMinutes
            type = mealWithDetails.meal.type
            customLabel = mealWithDetails.meal.customTypeLabel.orEmpty()
        } else {
            val defaultTime = vm.getDefaultTime(MealType.BREAKFAST)

            meal = Meal(
                dietId = dietId,
                dayOfWeek = dayOfWeek,
                type = MealType.LUNCH,
                timeMinutes = defaultTime,
            )

            timeMinutes = defaultTime
        }

        isLoading = false
    }

    fun saveMeal() {
        val currentMeal = meal ?: return

        val updatedMeal = currentMeal.copy(
            description = description.trim(),
            timeMinutes = timeMinutes,
            type = type,
            customTypeLabel = if (type == MealType.OTHER) {
                customLabel.trim().ifBlank { null }
            } else {
                null
            },
        )

        vm.saveMeal(updatedMeal, courses)
        onBack()
    }

    fun deleteMeal() {
        if (mealId == 0L) {
            return
        }

        vm.deleteMeal(mealId)
        onBack()
    }

    fun addCourse() {
        val currentMeal = meal ?: return

        courses = courses + CourseWithItems(
            course = Course(
                mealId = currentMeal.id,
                order = courses.size,
            ),
            items = emptyList(),
        )
    }

    MealDetailContent(
        isLoading = isLoading,
        isNew = mealId == 0L,
        type = type,
        timeMinutes = timeMinutes,
        customLabel = customLabel,
        description = description,
        courses = courses,
        onBack = onBack,
        onSave = ::saveMeal,
        onDelete = ::deleteMeal,
        onTimeClick = { showTimePicker = true },
        onTypeChange = { newType ->
            type = newType
            timeMinutes = vm.getDefaultTime(newType)
        },
        onCustomLabelChange = { customLabel = it },
        onDescriptionChange = { description = it },
        onAddCourse = ::addCourse,
        onUpdateCourse = { index, updated ->
            courses = courses.toMutableList().apply {
                set(index, updated)
            }
        },
        onDeleteCourse = { index ->
            courses = courses.toMutableList().apply {
                removeAt(index)
            }
        },
    )

    if (showTimePicker) {
        TimePickerDialog(
            title = stringResource(R.string.meal_time),
            initialTimeMinutes = timeMinutes,
            onDismiss = {
                showTimePicker = false
            },
            onTimeSelected = { selectedTime ->
                timeMinutes = selectedTime
                showTimePicker = false
            },
        )
    }
}

@Composable
fun MealDetailContent(
    isLoading: Boolean,
    isNew: Boolean,
    type: MealType,
    timeMinutes: Int,
    customLabel: String,
    description: String,
    courses: List<CourseWithItems>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTimeClick: () -> Unit,
    onTypeChange: (MealType) -> Unit,
    onCustomLabelChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddCourse: () -> Unit,
    onUpdateCourse: (Int, CourseWithItems) -> Unit,
    onDeleteCourse: (Int) -> Unit,
) {
    Scaffold { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
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
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 104.dp,
                ),
            ) {
                item {
                    MealHeader(
                        isNew = isNew,
                        type = type,
                        timeMinutes = timeMinutes,
                        onBack = onBack,
                        onSave = onSave,
                        onDelete = onDelete,
                    )
                }

                item {
                    MealInformationSection(
                        timeMinutes = timeMinutes,
                        onTimeClick = onTimeClick,
                        type = type,
                        onTypeChange = onTypeChange,
                    )
                }

                if (type == MealType.OTHER) {
                    item {
                        CustomLabelField(
                            value = customLabel,
                            onValueChange = onCustomLabelChange,
                        )
                    }
                }

                item {
                    MealNotesSection(
                        description = description,
                        onDescriptionChange = onDescriptionChange,
                    )
                }

                item {
                    CoursesHeader(count = courses.size)
                }

                if (courses.isEmpty()) {
                    item {
                        EmptyCourses(
                            onAddCourse = onAddCourse,
                        )
                    }
                } else {
                    item {
                        CoursesList(
                            courses = courses,
                            onUpdate = onUpdateCourse,
                            onDelete = onDeleteCourse,
                            onAddCourse = onAddCourse,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailContentPreview() {
    DietTheme {
        MealDetailContent(
            isLoading = false,
            isNew = false,
            type = MealType.LUNCH,
            timeMinutes = 780,
            customLabel = "",
            description = "Test notes",
            courses = listOf(
                CourseWithItems(
                    course = Course(id = 1, mealId = 1, name = "Pasta", order = 0),
                    items = listOf(
                        FoodItem(id = 1, courseId = 1, name = "Spaghetti", quantities = "100g", order = 0)
                    )
                )
            ),
            onBack = {},
            onSave = {},
            onDelete = {},
            onTimeClick = {},
            onTypeChange = {},
            onCustomLabelChange = {},
            onDescriptionChange = {},
            onAddCourse = {},
            onUpdateCourse = { _, _ -> },
            onDeleteCourse = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailContentLoadingPreview() {
    DietTheme {
        MealDetailContent(
            isLoading = true,
            isNew = true,
            type = MealType.BREAKFAST,
            timeMinutes = 480,
            customLabel = "",
            description = "",
            courses = emptyList(),
            onBack = {},
            onSave = {},
            onDelete = {},
            onTimeClick = {},
            onTypeChange = {},
            onCustomLabelChange = {},
            onDescriptionChange = {},
            onAddCourse = {},
            onUpdateCourse = { _, _ -> },
            onDeleteCourse = {},
        )
    }
}
