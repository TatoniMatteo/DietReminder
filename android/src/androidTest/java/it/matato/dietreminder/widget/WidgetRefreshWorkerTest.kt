package it.matato.dietreminder.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetRefreshWorkerTest {
    @Test fun periodicWorkerRefreshesRegisteredWidgetProvidersSuccessfully() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val worker=TestListenableWorkerBuilder<WidgetRefreshWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(),worker.doWork())
    }
}
