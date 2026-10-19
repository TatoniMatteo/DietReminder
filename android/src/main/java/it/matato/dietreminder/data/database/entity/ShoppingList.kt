package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingList(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val createdAt: Long = System.currentTimeMillis(),
	val dietId: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val remindersEnabled: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "1080") val reminderTimeMinutes: Int = 1080,
)
