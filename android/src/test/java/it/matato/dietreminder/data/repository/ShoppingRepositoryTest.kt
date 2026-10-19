package it.matato.dietreminder.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.repository.room.RoomShoppingListRepository
import it.matato.dietreminder.data.sync.WorkspaceCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=TestDietApplication::class)
class ShoppingRepositoryTest {
    @Test fun movingAllocationsAndTogglingParentsPreservesQuantitiesAndSyncMetadata() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(),AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao=db.shoppingListDao()
            val repo=RoomShoppingListRepository(db,dao,db.dietDao(),db.mealDao())
            val list=repo.createShoppingList("Spesa",null, emptyList())
            val item=dao.insertItem(ShoppingListItem(shoppingListId=list,name="Insalata",isFresh=true,category="Orto"))
            dao.insertItemDays(listOf(ShoppingListItemDay(id=1,shoppingListItemId=item,dayOfWeek=DayOfWeek.MONDAY,amount="150"),ShoppingListItemDay(id=2,shoppingListItemId=item,dayOfWeek=DayOfWeek.TUESDAY,amount="200")))
            repo.moveShoppingPurchase(item,1,DayOfWeek.SUNDAY)
            repo.toggleShoppingListItemBought(item,true)
            repo.updateShoppingReminders(list,true,1110)
            val snapshot=WorkspaceCache(db).read()
            assertEquals(1,snapshot.days.first().dayOfWeek)
            assertEquals(7,snapshot.days.first().purchaseDayOfWeek)
            assertEquals("150",snapshot.days.first().amount)
            assertTrue(snapshot.days.all { it.isBought })
            assertEquals("Orto",snapshot.items.first().category)
            assertEquals(1110,snapshot.lists.first().reminderTimeMinutes)
            repo.toggleShoppingListItemDayBought(1,false)
            assertFalse(WorkspaceCache(db).read().items.first().isBought)
        } finally { db.close() }
    }
    @Test fun schemaNineMigratesWithoutDroppingExistingShoppingData() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val name="shopping-migration-test.db"
        context.deleteDatabase(name)
        val original=AppDatabase.create(context,name)
        original.shoppingListDao().insertList(ShoppingList(id=1,name="Da conservare"))
        original.shoppingListDao().insertItem(ShoppingListItem(id=1,shoppingListId=1,name="Mele",amount="2"))
        original.close()
        val sqlite=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
        sqlite.execSQL("PRAGMA foreign_keys=OFF")
        val definitions=mapOf(
            "shopping_list" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `dietId` INTEGER",
            "shopping_list_item" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shoppingListId` INTEGER NOT NULL, `name` TEXT NOT NULL, `amount` TEXT NOT NULL, `unit` TEXT NOT NULL, `isBought` INTEGER NOT NULL, `isCustom` INTEGER NOT NULL, `isFresh` INTEGER NOT NULL, `dayOfWeek` INTEGER, FOREIGN KEY(`shoppingListId`) REFERENCES `shopping_list`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE",
            "shopping_list_item_day" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shoppingListItemId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `amount` TEXT NOT NULL, `unit` TEXT NOT NULL, `isBought` INTEGER NOT NULL, FOREIGN KEY(`shoppingListItemId`) REFERENCES `shopping_list_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
        )
        for ((table,definition) in definitions) {
            val columns=definition.substringBefore(", FOREIGN KEY").split(", ").map { it.substringBefore(' ') }.joinToString(",")
            sqlite.execSQL("CREATE TABLE ${table}_old ($definition)")
            sqlite.execSQL("INSERT INTO ${table}_old ($columns) SELECT $columns FROM $table")
            sqlite.execSQL("DROP TABLE $table")
            sqlite.execSQL("ALTER TABLE ${table}_old RENAME TO $table")
        }
        sqlite.execSQL("CREATE INDEX index_shopping_list_item_shoppingListId ON shopping_list_item(shoppingListId)")
        sqlite.execSQL("CREATE INDEX index_shopping_list_item_day_shoppingListItemId ON shopping_list_item_day(shoppingListItemId)")
        sqlite.version=9
        sqlite.close()
        val migrated=AppDatabase.create(context,name)
        try {
            val snapshot=WorkspaceCache(migrated).read()
            assertEquals("Da conservare",snapshot.lists.single().name)
            assertEquals("Mele",snapshot.items.single().name)
            assertFalse(snapshot.lists.single().remindersEnabled)
            assertNull(snapshot.items.single().category)
        } finally { migrated.close();context.deleteDatabase(name) }
    }
}
