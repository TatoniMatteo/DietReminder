package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.util.UUID

@Entity(tableName = "diets")
data class Diet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val nextMealWindowMinutes: Int = 90,
    val isActive: Boolean = false,
    val disabledNotificationDays: String = ""
) {
    fun isDayNotificationEnabled(day: DayOfWeek): Boolean {
        if (disabledNotificationDays.isBlank()) return true
        val disabledSet = disabledNotificationDays.split(",")
            .mapNotNull { runCatching { DayOfWeek.valueOf(it.trim()) }.getOrNull() }
            .toSet()
        return day !in disabledSet
    }

    fun withDayNotificationToggled(day: DayOfWeek, enabled: Boolean): Diet {
        val disabledSet = if (disabledNotificationDays.isBlank()) {
            mutableSetOf()
        } else {
            disabledNotificationDays.split(",")
                .mapNotNull { runCatching { DayOfWeek.valueOf(it.trim()) }.getOrNull() }
                .toMutableSet()
        }

        if (enabled) {
            disabledSet.remove(day)
        } else {
            disabledSet.add(day)
        }

        return copy(disabledNotificationDays = disabledSet.joinToString(",") { it.name })
    }
}
