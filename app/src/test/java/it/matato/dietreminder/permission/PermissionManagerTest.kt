package it.matato.dietreminder.permission

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class PermissionManagerTest {

    @Test
    fun notificationPermission_returnsPostNotificationsString() {
        assertEquals(Manifest.permission.POST_NOTIFICATIONS, PermissionManager.notificationPermission())
    }

    @Test
    fun getMissingRuntimePermissions_returnsList() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val missing = PermissionManager.getMissingRuntimePermissions(context)
        assertNotNull(missing)
    }

    @Test
    fun getMissingSpecialPermissions_returnsList() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val missing = PermissionManager.getMissingSpecialPermissions(context)
        assertNotNull(missing)
    }

    @Test
    fun createSpecialPermissionIntent_returnsValidIntent() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = PermissionManager.createSpecialPermissionIntent(
            context,
            PermissionManager.SpecialPermission.EXACT_ALARM
        )

        assertNotNull(intent)
        assertEquals("package:${context.packageName}", intent?.data.toString())
    }
}
