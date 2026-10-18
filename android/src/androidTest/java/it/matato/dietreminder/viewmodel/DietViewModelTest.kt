package it.matato.dietreminder.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import kotlinx.coroutines.withTimeout
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
		withTimeout(5000) { viewModel.versionStatus.first { !it.isChecking } }
		viewModel.create("Test Instrumented Diet", 60)
		val diets = withTimeout(5000) { repository.all.first { it.size == 1 } }
		assertEquals(1, diets.size)
		assertEquals("Test Instrumented Diet", diets[0].name)
	}
}
