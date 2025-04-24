package sweng894.project.adopto.profile

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R
import sweng894.project.adopto.data.ExplorationPreferences

@RunWith(AndroidJUnit4::class)
@LargeTest
class ExplorePreferencesActivityTest {

    private fun launchWithMockPrefs(): ActivityScenario<ExplorePreferencesActivity> {
        val prefs = ExplorationPreferences(
            min_animal_age = 1.0,
            max_animal_age = 5.0,
            animal_sizes = mutableListOf("Small", "Medium"),
            animal_types = mutableListOf("Dog"),
            search_radius_miles = 20.0
        )

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(
                "sweng894.project.adopto",
                "sweng894.project.adopto.profile.ExplorePreferencesActivity"
            )
            putExtra("explore_preferences", prefs)
        }

        return ActivityScenario.launch(intent)
    }

    @Test
    fun allUiElementsAreVisible() {
        launchWithMockPrefs()

        onView(withId(R.id.search_radius_slider)).check(matches(isDisplayed()))
        onView(withId(R.id.age_range_slider)).check(matches(isDisplayed()))
        onView(withId(R.id.sizes_multi_select)).check(matches(isDisplayed()))
        onView(withId(R.id.types_multi_select)).check(matches(isDisplayed()))
        onView(withId(R.id.save_preferences_button)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel_button)).check(matches(isDisplayed()))
    }

    @Test
    fun saveButtonInitiallyDisabled_andEnablesOnSliderChange() {
        launchWithMockPrefs()

        onView(withId(R.id.save_preferences_button)).check(matches(not(isEnabled())))

        // Simulate slider interaction
        onView(withId(R.id.search_radius_slider)).perform(swipeRight())

        onView(withId(R.id.save_preferences_button)).check(matches(isEnabled()))
    }

    @Test
    fun cancelButtonFinishesActivity() {
        val scenario = launchWithMockPrefs()

        onView(withId(R.id.cancel_button)).perform(click())

        scenario.onActivity {
            assert(it.isFinishing || it.isDestroyed)
        }
    }

    @Test
    fun ageSliderChangeEnablesSaveButton() {
        launchWithMockPrefs()

        onView(withId(R.id.age_range_slider)).perform(swipeRight())

        onView(withId(R.id.save_preferences_button)).check(matches(isEnabled()))
    }
}
