package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "diets")
data class Diet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val nextMealWindowMinutes: Int = 90,
    val isActive: Boolean = false
)