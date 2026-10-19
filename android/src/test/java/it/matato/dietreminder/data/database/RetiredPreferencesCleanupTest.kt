package it.matato.dietreminder.data.database

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Diet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class RetiredPreferencesCleanupTest {
    @Test
    fun upgradeRemovesRetiredPreferencesAndKeepsDietsAndTheme() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.deleteDatabase("diet.db")
        val keys = listOf("HYDRATION_ENABLED", "HYDRATION_RANGES", "HYDRATION_DAYS", "HYDRATION_INTERVAL")
        val dietId = withDatabase(context) { db ->
            keys.forEach { db.configDao().insertConfig(AppConfig(it, "legacy")) }
            db.configDao().insertConfig(AppConfig(ConfigKey.THEME, "dark"))
            db.dietDao().insert(Diet(name = "My diet"))
        }
        try {
            repeat(2) {
                withDatabase(context) { db ->
                    keys.forEach { assertNull(db.configDao().observeConfig(it).first()) }
                    assertEquals("dark", db.configDao().observeConfig(ConfigKey.THEME).first()?.value)
                    assertEquals("My diet", db.dietDao().get(dietId)?.name)
                }
            }
        } finally {
            context.deleteDatabase("diet.db")
        }
    }

    private suspend fun <T> withDatabase(context: Context, block: suspend (AppDatabase) -> T): T {
        val db = AppDatabase.create(context)
        return try {
            block(db)
        } finally {
            db.close()
        }
    }

}
