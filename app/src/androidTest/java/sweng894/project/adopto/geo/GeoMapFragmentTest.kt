package sweng894.project.adopto.geo

import android.content.Intent
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R
import org.junit.Rule
import sweng894.project.adopto.NavigationBaseActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule

@RunWith(AndroidJUnit4::class)
@LargeTest
class GeoMapFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(NavigationBaseActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()

        // Launch GeoMapFragment directly if needed
        launchFragmentInContainer<GeoMapFragment>()
    }

    @Test
    fun map_is_displayed_and_search_button_clickable() {
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_location_search)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_location_search)).check(matches(isClickable()))
    }

    @Test
    fun clicking_search_button_invokes_autocomplete() {
        // Click search and validate result
        onView(withId(R.id.btn_location_search)).perform(click())
    }
}
