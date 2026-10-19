package it.matato.dietreminder.widget

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootReceiverTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun bootSchedulesOnePeriodicRefreshEvenWhenBroadcastIsRepeated() {
        val receiver=BootReceiver()
        receiver.onReceive(context,Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context,Intent(Intent.ACTION_BOOT_COMPLETED))
        val work=WorkManager.getInstance(context).getWorkInfosForUniqueWork(WidgetRefreshWorker.WORK_NAME).get().filter { !it.state.isFinished }
        assertEquals(1,work.size)
        assertNotNull(work.single().periodicityInfo)
    }
    @Test fun unrelatedBroadcastDoesNotChangeRefreshSchedule() {
        val manager=WorkManager.getInstance(context)
        val before=manager.getWorkInfosForUniqueWork(WidgetRefreshWorker.WORK_NAME).get().map { it.id }
        BootReceiver().onReceive(context,Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        assertEquals(before,manager.getWorkInfosForUniqueWork(WidgetRefreshWorker.WORK_NAME).get().map { it.id })
    }
}
