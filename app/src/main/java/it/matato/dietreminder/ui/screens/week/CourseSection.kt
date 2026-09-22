package it.matato.dietreminder.ui.screens.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun CourseSection(course: CourseWithItems) {
    Column {
        Text(
            text = course.course.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.size(6.dp))

        ItemRows(course)
    }
}

@Composable
private fun ItemRows(course: CourseWithItems) {
    course.items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )

            Spacer(modifier = Modifier.width(9.dp))

            Text(
                text = buildString {
                    append(item.name)

                    if (item.quantities.isNotEmpty()) {
                        append("  ")
                        append(item.quantities)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CourseSection(
                course = CourseWithItems(
                    course = Course(id = 1, mealId = 1, name = "Main Course"),
                    items = listOf(
                        FoodItem(id = 1, courseId = 1, name = "Pasta", quantities = "100g"),
                        FoodItem(id = 2, courseId = 1, name = "Tomato Sauce", quantities = "50ml"),
                    )
                )
            )
        }
    }
}