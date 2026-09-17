package it.matato.dietreminder.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.domain.NextMeal
import it.matato.dietreminder.domain.nextMeal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

class DietReminder : GlanceAppWidget() {

    companion object {
        private val SMALL_SQUARE = DpSize(64.dp, 64.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(180.dp, 64.dp)
        private val BIG_SQUARE = DpSize(180.dp, 180.dp)

        suspend fun updateAll(context: Context) {
            DietReminder().updateAll(context)
        }
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SQUARE,
            HORIZONTAL_RECTANGLE,
            BIG_SQUARE
        )
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val app = context.applicationContext as DietApplication
        val diet = app.repository.active.first()
        val meals = diet?.let { app.repository.getMeals(it.id) }.orEmpty()
        val next = diet?.let {
            nextMeal(
                meals = meals,
                now = LocalDateTime.now(),
                windowMinutes = it.nextMealWindowMinutes
            )
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            "dietreminder://week".toUri()
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        provideContent {
            val size = LocalSize.current

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(24.dp)
                        .clickable(actionStartActivity(intent))
                ) {
                    when {
                        size.width <= 100.dp && size.height <= 100.dp -> {
                            SmallContent(
                                context = context,
                                next = next
                            )
                        }

                        size.height <= 100.dp -> {
                            HorizontalContent(
                                context = context,
                                next = next
                            )
                        }

                        else -> {
                            BigContent(
                                context = context,
                                name = diet?.name ?: context.getString(R.string.no_diet),
                                next = next
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallContent(
        context: Context,
        next: NextMeal?
    ) {
        val mealIcon = getMealIcon(next)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(7.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (next != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(mealIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp)
                    )

                    Spacer(GlanceModifier.height(2.dp))

                    Text(
                        text = getMealShortLabel(context, next),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onPrimaryContainer
                        ),
                        maxLines = 1
                    )

                    Text(
                        text = formatTime(next),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onPrimaryContainer
                        )
                    )
                }
            } else {
                Image(
                    provider = ImageProvider(R.drawable.ic_no_meal),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    private fun HorizontalContent(
        context: Context,
        next: NextMeal?
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(getMealIcon(next)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
            }

            Spacer(GlanceModifier.width(10.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.next_meal),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )

                Text(
                    text = getMealLabel(context, next),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
            }

            if (next != null) {
                Text(
                    text = formatTime(next),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    ),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun BigContent(
        context: Context,
        name: String,
        next: NextMeal?
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(42.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(getMealIcon(next)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp)
                    )
                }

                Spacer(GlanceModifier.width(10.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.next_meal),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )

                    Text(
                        text = getMealLabel(context, next),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                }

                if (next != null) {
                    Text(
                        text = formatTime(next),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(GlanceModifier.height(12.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = name,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSecondaryContainer
                    ),
                    maxLines = 1
                )
            }

            if (next != null && next.meal.courses.isNotEmpty()) {
                Spacer(GlanceModifier.height(12.dp))

                next.meal.courses.take(4).forEach { course ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(5.dp)
                                .background(GlanceTheme.colors.primary)
                                .cornerRadius(3.dp)
                        ) {}

                        Spacer(GlanceModifier.width(8.dp))

                        Text(
                            text = course.course.name,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }

                if (next.meal.courses.size > 4) {
                    Text(
                        text = "...",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.padding(start = 13.dp)
                    )
                }
            }
        }
    }

    private fun getMealLabel(
        context: Context,
        next: NextMeal?
    ): String {
        return next?.meal?.meal?.customTypeLabel
            ?: next?.meal?.meal?.type?.let { type ->
                val resId = when (type) {
                    MealType.BREAKFAST -> R.string.meal_breakfast
                    MealType.MORNING_SNACK -> R.string.meal_morning_snack
                    MealType.LUNCH -> R.string.meal_lunch
                    MealType.AFTERNOON_SNACK -> R.string.meal_afternoon_snack
                    MealType.DINNER -> R.string.meal_dinner
                    MealType.OTHER -> R.string.meal_other
                }

                context.getString(resId)
            }
            ?: context.getString(R.string.configure_diet)
    }

    private fun getMealShortLabel(
        context: Context,
        next: NextMeal?
    ): String {
        return when (next?.meal?.meal?.type) {
            MealType.BREAKFAST -> context.getString(R.string.meal_breakfast_short)
            MealType.MORNING_SNACK -> context.getString(R.string.meal_morning_snack_short)
            MealType.LUNCH -> context.getString(R.string.meal_lunch_short)
            MealType.AFTERNOON_SNACK -> context.getString(R.string.meal_afternoon_snack_short)
            MealType.DINNER -> context.getString(R.string.meal_dinner_short)
            else -> context.getString(R.string.meal_other_short)
        }
    }

    private fun getMealIcon(next: NextMeal?): Int {
        return when (next?.meal?.meal?.type) {
            MealType.BREAKFAST -> R.drawable.ic_breakfast
            MealType.MORNING_SNACK -> R.drawable.ic_morning_snack
            MealType.LUNCH -> R.drawable.ic_lunch
            MealType.AFTERNOON_SNACK -> R.drawable.ic_afternoon_snack
            MealType.DINNER -> R.drawable.ic_dinner
            MealType.OTHER -> R.drawable.ic_other_meal
            else -> R.drawable.ic_no_meal
        }
    }

    private fun formatTime(next: NextMeal): String {
        val hour = next.meal.meal.timeMinutes / 60
        val minute = next.meal.meal.timeMinutes % 60

        return "%02d:%02d".format(hour, minute)
    }
}

class DietReminderReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DietReminder()
}