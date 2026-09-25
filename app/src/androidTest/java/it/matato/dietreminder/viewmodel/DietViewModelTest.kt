package it.matato.dietreminder.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.FakeDietRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietViewModelTest {

    @Test
    fun testViewModelInitializationAndDietCreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repository = FakeDietRepository()
        val viewModel = DietViewModel(context, repository)

        assertTrue(repository.all.first().isEmpty())
        viewModel.create("Test Instrumented Diet", 60)
        delay(100.milliseconds)

        val diets = repository.all.first()
        assertEquals(1, diets.size)
        assertEquals("Test Instrumented Diet", diets[0].name)
    }
}
