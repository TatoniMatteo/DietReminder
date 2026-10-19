package it.matato.dietreminder.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.repository.contracts.*
import it.matato.dietreminder.data.repository.room.*
import it.matato.dietreminder.data.repository.cache.*
import it.matato.dietreminder.data.repository.delegating.*
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import it.matato.dietreminder.data.sync.*

@Singleton
class AccountRepositories @Inject constructor(@ApplicationContext private val context: Context, private val sessions: AccountSessionStore, private val scheduler: CloudSyncScheduler? = null) {
    data class Repositories(val diets: DietRepository, val shopping: ShoppingListRepository, val config: ConfigRepository, val journal: SyncJournal? = null, val database: AppDatabase? = null)
    private val repositories = mutableMapOf<String, Repositories>()
    val changes = sessions.state.map { forScope(it.scope) }.distinctUntilChanged()
    val current: Repositories get() = forScope(sessions.state.value.scope)

    @Synchronized
    fun forScope(scope: String): Repositories = repositories.getOrPut(scope) {
        val db = AppDatabase.create(context, AccountSessionStore.databaseName(scope))
        val diets = RoomDietRepository(db, db.dietDao(), db.mealDao(), db.courseDao(), db.foodItemDao())
        val shopping = RoomShoppingListRepository(db, db.shoppingListDao(), db.dietDao(), db.mealDao())
        val config = RoomConfigRepository(db, db.configDao(), db.dietDao(), db.shoppingListDao())
        if (scope != "local" && scope != "locked") {
            val journal = SyncJournal(db, { scheduler?.enqueue(scope, replace = true) }) {
                sessions.state.value.scope == scope && sessions.token() != null
            }
            val shared = CloudSharedRepository(diets, shopping, config, journal) { permission ->
                sessions.state.value.scope == scope && sessions.state.value.session?.account?.permits(permission) == true
            }
            Repositories(shared, shared, shared, journal, db)
        } else {
            Repositories(
                DelegatingDietRepository(diets, RoomDietCacheRepository(diets)) { scope == "locked" || UpdateManagerStatus.writesBlocked },
                DelegatingShoppingListRepository(shopping, RoomShoppingListCacheRepository(shopping)) { scope == "locked" || UpdateManagerStatus.writesBlocked },
                config, database = db
            )
        }
    }
}
