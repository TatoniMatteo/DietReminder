package it.matato.dietreminder.widget

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=TestDietApplication::class)
class WidgetWritePolicyTest {
    private var checking=true
    private var offline=true
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    @Before fun capture() { checking=UpdateManagerStatus.isChecking;offline=UpdateManagerStatus.isOffline }
    @After fun restore() { UpdateManagerStatus.isChecking=checking;UpdateManagerStatus.isOffline=offline }
    private fun policy(offline: Boolean)=object:VersionPolicyRepository { override suspend fun fetchPolicy()=VersionPolicy() to offline }
    @Test fun widgetColdStartChecksCompatibilityWithoutOpeningTheApplication()=runBlocking {
        UpdateManagerStatus.isChecking=true;UpdateManagerStatus.isOffline=true
        WidgetWritePolicy.ensureInitialized(context,policy(false))
        assertFalse(UpdateManagerStatus.isChecking);assertFalse(UpdateManagerStatus.writesBlocked)
    }
    @Test fun unavailableServerKeepsWidgetWritesBlocked()=runBlocking {
        UpdateManagerStatus.isChecking=true
        WidgetWritePolicy.ensureInitialized(context,policy(true))
        assertFalse(UpdateManagerStatus.isChecking);assertTrue(UpdateManagerStatus.writesBlocked)
    }
    @Test fun alreadyInitializedApplicationDoesNotStartAnotherNetworkRequest()=runBlocking {
        UpdateManagerStatus.isChecking=false;UpdateManagerStatus.isOffline=false
        WidgetWritePolicy.ensureInitialized(context,object:VersionPolicyRepository { override suspend fun fetchPolicy():Pair<VersionPolicy,Boolean> = error("Unexpected request") })
        assertFalse(UpdateManagerStatus.writesBlocked)
    }
}
