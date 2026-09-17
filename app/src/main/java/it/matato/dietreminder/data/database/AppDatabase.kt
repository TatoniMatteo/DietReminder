package it.matato.dietreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.matato.dietreminder.data.database.dao.ConfigDao
import it.matato.dietreminder.data.database.dao.CourseDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.FoodItemDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.entity.Meal

@Database(
    entities = [
        Diet::class,
        Meal::class,
        Course::class,
        FoodItem::class,
        MealDefaultTime::class,
        AppConfig::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dietDao(): DietDao

    abstract fun mealDao(): MealDao

    abstract fun courseDao(): CourseDao

    abstract fun foodItemDao(): FoodItemDao

    abstract fun configDao(): ConfigDao

    companion object {

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "diet.db"
            )
                .fallbackToDestructiveMigration(true)
                .build()
    }
}