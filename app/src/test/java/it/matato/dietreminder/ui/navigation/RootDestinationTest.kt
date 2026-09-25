package it.matato.dietreminder.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootDestinationTest {

    @Test
    fun rootDestinations_routeReturnsValidRouteObjects() {
        assertNotNull(RootDestination.WEEK.route())
        assertNotNull(RootDestination.DIETS.route())
        assertNotNull(RootDestination.HYDRATION.route())
        assertNotNull(RootDestination.SETTINGS.route())
    }

    @Test
    fun rootDestinations_matchesRouteStrings() {
        assertTrue(RootDestination.WEEK.matches("it.matato.dietreminder.ui.navigation.WeekRoute"))
        assertTrue(RootDestination.DIETS.matches("it.matato.dietreminder.ui.navigation.DietsRoute"))
        assertTrue(RootDestination.HYDRATION.matches("it.matato.dietreminder.ui.navigation.HydrationRoute"))
        assertTrue(RootDestination.SETTINGS.matches("it.matato.dietreminder.ui.navigation.SettingsRoute"))

        assertFalse(RootDestination.WEEK.matches("UnknownRoute"))
        assertFalse(RootDestination.WEEK.matches(null))
    }
}
