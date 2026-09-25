package it.matato.dietreminder.data.database.entity

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppConfigEntityTest {
    @Test
    fun testAppConfigCreation() {
        val config = AppConfig(key = ConfigKey.THEME, value = "dark")
        assertEquals(ConfigKey.THEME.name, config.key)
        assertEquals("dark", config.value)
    }
}
