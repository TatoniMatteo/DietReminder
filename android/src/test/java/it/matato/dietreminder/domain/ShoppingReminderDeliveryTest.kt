package it.matato.dietreminder.domain

import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.util.shopping.ShoppingReminderNotifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=TestDietApplication::class)
class ShoppingReminderDeliveryTest {
    private val snapshot=WorkspaceSnapshot(lists=listOf(CloudShoppingList(1,"Spesa",1,remindersEnabled=true)),items=listOf(CloudShoppingItem(1,1,"Mele",dayOfWeek=1)))
    @Test fun deliversOnSundayForMondayOnceAndClearsWhenBought() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val now=LocalDateTime.of(2026,10,18,18,30)
        assertEquals(0, ShoppingReminderNotifier.deliver(context,snapshot,"local",now.minusHours(1)){true})
        assertEquals(1, ShoppingReminderNotifier.deliver(context,snapshot,"local",now){true})
        val manager=context.getSystemService(NotificationManager::class.java)
        assertEquals(1,manager.activeNotifications.size)
        assertTrue(manager.activeNotifications.single().notification.extras.getCharSequence("android.text").toString().contains("Mele"))
        assertEquals(0, ShoppingReminderNotifier.deliver(context,snapshot,"local",now){true})
        ShoppingReminderNotifier.deliver(context,snapshot.copy(items=snapshot.items.map{it.copy(isBought=true)}),"local",now){true}
        assertEquals(0,manager.activeNotifications.size)
    }
    @Test fun disabledListsAndSwitchedAccountsNeverNotify() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val now=LocalDateTime.of(2026,10,18,18,30)
        assertEquals(0, ShoppingReminderNotifier.deliver(context,snapshot,"previous-account",now){false})
        assertEquals(0, ShoppingReminderNotifier.deliver(context,snapshot.copy(lists=snapshot.lists.map{it.copy(remindersEnabled=false)}),"local",now){true})
    }

    @Test fun commonTimeOverridesDifferentLegacyListTimes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val now = LocalDateTime.of(2026, 10, 18, 17, 0)
        val lists = snapshot.copy(
            lists = listOf(
                snapshot.lists.single().copy(reminderTimeMinutes = 480),
                CloudShoppingList(2, "Altra spesa", 1, remindersEnabled = true, reminderTimeMinutes = 1320),
            ),
            items = snapshot.items + CloudShoppingItem(2, 2, "Pane", dayOfWeek = 1),
        )
        assertEquals(0, ShoppingReminderNotifier.deliver(context, lists, "local", now.minusMinutes(1), 1020) { true })
        assertEquals(2, ShoppingReminderNotifier.deliver(context, lists, "local", now, 1020) { true })
        assertEquals(0, ShoppingReminderNotifier.deliver(context, lists, "local", now.plusMinutes(30), 1020) { true })
    }
}
