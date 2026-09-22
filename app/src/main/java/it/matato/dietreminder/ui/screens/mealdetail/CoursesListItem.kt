package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun CourseListItem(
    index: Int,
    courseWithItems: CourseWithItems,
    onUpdate: (CourseWithItems) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 14.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = courseWithItems.course.name,
                onValueChange = {
                    onUpdate(
                        courseWithItems.copy(
                            course = courseWithItems.course.copy(name = it),
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text(stringResource(R.string.course_name_hint))
                },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        courseWithItems.items.forEachIndexed { itemIndex, item ->
            FoodItemRow(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = courseWithItems.items.toMutableList().apply {
                        set(itemIndex, updatedItem)
                    }

                    onUpdate(
                        courseWithItems.copy(items = updatedItems),
                    )
                },
                onDelete = {
                    val updatedItems = courseWithItems.items.toMutableList().apply {
                        removeAt(itemIndex)
                    }

                    onUpdate(
                        courseWithItems.copy(items = updatedItems),
                    )
                },
            )

            if (itemIndex < courseWithItems.items.lastIndex) {
                HorizontalDivider()
            }
        }

        TextButton(
            onClick = {
                val newItem = FoodItem(
                    courseId = courseWithItems.course.id,
                    name = "",
                    quantities = "",
                    order = courseWithItems.items.size,
                )

                onUpdate(
                    courseWithItems.copy(
                        items = courseWithItems.items + newItem,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.add_food_item))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseListItemPreview() {
    DietTheme {
        CourseListItem(
            index = 0,
            courseWithItems = CourseWithItems(
                course = Course(id = 1, mealId = 1, name = "Pasta"),
                items = listOf(
                    FoodItem(id = 1, courseId = 1, name = "Spaghetti", quantities = "100g"),
                    FoodItem(id = 2, courseId = 1, name = "Tomato Sauce", quantities = "50g"),
                ),
            ),
            onUpdate = {},
            onDelete = {},
        )
    }
}
