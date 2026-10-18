package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Update suspend fun updateDay(day: it.matato.dietreminder.data.database.entity.ShoppingListItemDay)
    @Query("UPDATE shopping_list_item_day SET isBought = :bought WHERE shoppingListItemId = :itemId")
    suspend fun updateAllDaysBought(itemId: Long, bought: Boolean)
    @Query("UPDATE shopping_list_item_day SET purchaseDayOfWeek = :day WHERE id = :dayId AND shoppingListItemId = :itemId")
    suspend fun moveDay(itemId: Long, dayId: Long, day: java.time.DayOfWeek?)
    @Query("UPDATE shopping_list_item SET purchaseDayOfWeek = :day WHERE id = :itemId")
    suspend fun moveItem(itemId: Long, day: java.time.DayOfWeek?)

    @Query("SELECT * FROM shopping_list ORDER BY id") suspend fun listsForSync(): List<ShoppingList>
    @Query("SELECT * FROM shopping_list_item ORDER BY id") suspend fun itemsForSync(): List<ShoppingListItem>
    @Query("SELECT * FROM shopping_list_item_day ORDER BY id") suspend fun daysForSync(): List<ShoppingListItemDay>

	@Transaction
	@Query("SELECT * FROM shopping_list ORDER BY createdAt DESC")
	fun observeAllWithItems(): Flow<List<ShoppingListWithItems>>

	@Transaction
	@Query("SELECT * FROM shopping_list WHERE id = :id")
	fun observeWithItems(id: Long): Flow<ShoppingListWithItems?>

	@Transaction
	@Query("SELECT * FROM shopping_list WHERE id = :id")
	suspend fun getWithItems(id: Long): ShoppingListWithItems?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertList(list: ShoppingList): Long

	@Update
	suspend fun updateList(list: ShoppingList)

	@Query("DELETE FROM shopping_list WHERE id = :id")
	suspend fun deleteList(id: Long)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertItem(item: ShoppingListItem): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertItems(items: List<ShoppingListItem>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertItemDays(days: List<ShoppingListItemDay>)

	@Update
	suspend fun updateItem(item: ShoppingListItem)

	@Query("DELETE FROM shopping_list_item WHERE id = :id")
	suspend fun deleteItem(id: Long)

	@Query("UPDATE shopping_list_item SET isBought = :isBought WHERE id = :id")
	suspend fun updateBoughtStatus(id: Long, isBought: Boolean)

	@Query("UPDATE shopping_list_item_day SET isBought = :isBought WHERE id = :id")
	suspend fun updateDayBoughtStatus(id: Long, isBought: Boolean)

	@Query("SELECT shoppingListItemId FROM shopping_list_item_day WHERE id = :dayId")
	suspend fun getItemIdForDay(dayId: Long): Long?

	@Query("SELECT * FROM shopping_list_item_day WHERE shoppingListItemId = :itemId")
	suspend fun getItemDays(itemId: Long): List<ShoppingListItemDay>

	@Query("DELETE FROM shopping_list")
	suspend fun deleteAllLists()

	@Query("DELETE FROM shopping_list_item")
	suspend fun deleteAllItems()

	@Query("DELETE FROM shopping_list_item_day")
	suspend fun deleteAllItemDays()
}
