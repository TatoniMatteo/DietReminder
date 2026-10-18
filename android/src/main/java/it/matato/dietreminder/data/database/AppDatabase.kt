package it.matato.dietreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import it.matato.dietreminder.data.database.dao.ConfigDao
import it.matato.dietreminder.data.database.dao.CourseDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.FoodItemDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.dao.ShoppingListDao
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay

@Database(
	entities = [
		Diet::class,
		Meal::class,
		Course::class,
		FoodItem::class,
		MealDefaultTime::class,
		AppConfig::class,
		ShoppingList::class,
		ShoppingListItem::class,
		ShoppingListItemDay::class,
	],
	version = 10,
	exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

	abstract fun dietDao(): DietDao

	abstract fun mealDao(): MealDao

	abstract fun courseDao(): CourseDao

	abstract fun foodItemDao(): FoodItemDao

	abstract fun configDao(): ConfigDao

	abstract fun shoppingListDao(): ShoppingListDao

	companion object {

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN reminderTimeMinutes INTEGER NOT NULL DEFAULT 1080")
                db.execSQL("ALTER TABLE shopping_list_item ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE shopping_list_item ADD COLUMN purchaseDayOfWeek INTEGER")
                db.execSQL("ALTER TABLE shopping_list_item_day ADD COLUMN purchaseDayOfWeek INTEGER")
            }
        }

		fun create(context: Context, name: String = "diet.db"): AppDatabase =
			Room.databaseBuilder(
				context,
				AppDatabase::class.java,
				name
			)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // Keys are stored as strings; no schema change or destructive migration is needed.
                        db.execSQL("DELETE FROM app_config WHERE `key` IN ('HYDRATION_ENABLED', 'HYDRATION_RANGES', 'HYDRATION_DAYS', 'HYDRATION_INTERVAL')")
                    }
                })
				.addMigrations(MIGRATION_9_10)
				.build()
	}
}