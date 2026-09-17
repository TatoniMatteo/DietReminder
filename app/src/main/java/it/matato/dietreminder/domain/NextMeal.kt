package it.matato.dietreminder.domain

import it.matato.dietreminder.data.MealWithDetails
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class NextMeal(val meal: MealWithDetails, val dateTime: LocalDateTime, val timeRemainingMinutes: Long)

fun nextMeal(meals: List<MealWithDetails>, now: LocalDateTime, windowMinutes: Int): NextMeal? {
    if (meals.isEmpty()) return null
    val window = windowMinutes.coerceAtLeast(0).toLong()
    val candidates = (0 .. 1).flatMap { dayOffset ->
        val date = now.toLocalDate().plusDays(dayOffset.toLong())
        meals.filter { it.meal.dayOfWeek == date.dayOfWeek }.map { meal ->
            val mealDateTime = LocalDateTime.of(date, LocalTime.of(meal.meal.timeMinutes / 60, meal.meal.timeMinutes % 60))
            Candidate(meal = meal, mealDateTime = mealDateTime, showUntil = mealDateTime.plusMinutes(window))
        }
    }.sortedBy { it.mealDateTime }
    val current =
        candidates.firstOrNull { candidate -> !now.isBefore(candidate.mealDateTime) && now.isBefore(candidate.showUntil) }
    val next = current ?: candidates.firstOrNull { candidate -> candidate.mealDateTime.isAfter(now) }
    val selected = current ?: next ?: return null

    return NextMeal(
        meal = selected.meal,
        dateTime = selected.mealDateTime,
        timeRemainingMinutes = ChronoUnit.MINUTES.between(now, selected.mealDateTime)
    )
}

private data class Candidate(val meal: MealWithDetails, val mealDateTime: LocalDateTime, val showUntil: LocalDateTime)