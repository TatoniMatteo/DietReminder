package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun CoursesHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            title = stringResource(R.string.courses),
            icon = Icons.Rounded.Restaurant,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = 10.dp,
                    vertical = 5.dp,
                ),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CoursesList(
    courses: List<CourseWithItems>,
    onUpdate: (Int, CourseWithItems) -> Unit,
    onDelete: (Int) -> Unit,
    onAddCourse: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            courses.forEachIndexed { index, course ->
                CourseListItem(
                    index = index,
                    courseWithItems = course,
                    onUpdate = {
                        onUpdate(index, it)
                    },
                    onDelete = {
                        onDelete(index)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = if (index < courses.lastIndex) 16.dp else 0.dp,
                    ),
                )
            }

            TextButton(
                onClick = onAddCourse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_course))
            }
        }
    }
}

@Composable
fun EmptyCourses(
    onAddCourse: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddCourse),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconContainer(
                icon = Icons.Rounded.Restaurant,
                size = 56.dp,
                iconSize = 28.dp,
            )

            Text(
                text = stringResource(R.string.courses),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.add_course),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoursesHeaderPreview() {
    DietTheme {
        CoursesHeader(count = 2)
    }
}

@Preview(showBackground = true)
@Composable
private fun CoursesListPreview() {
    DietTheme {
        CoursesList(
            courses = listOf(
                CourseWithItems(
                    course = Course(id = 1, mealId = 1, name = "Course 1"),
                    items = listOf(
                        FoodItem(id = 1, courseId = 1, name = "Item 1", quantities = "100g"),
                    ),
                ),
                CourseWithItems(
                    course = Course(id = 2, mealId = 1, name = "Course 2"),
                    items = emptyList(),
                ),
            ),
            onUpdate = { _, _ -> },
            onDelete = {},
            onAddCourse = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyCoursesPreview() {
    DietTheme {
        EmptyCourses(onAddCourse = {})
    }
}
