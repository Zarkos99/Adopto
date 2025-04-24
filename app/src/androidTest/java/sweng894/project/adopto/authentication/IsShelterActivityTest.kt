package sweng894.project.adopto.authentication

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
@LargeTest
class IsShelterActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(IsShelterActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun buttonsAreDisplayed() {
        onView(withId(R.id.yes_button)).check(matches(isDisplayed()))
        onView(withId(R.id.no_button)).check(matches(isDisplayed()))
    }

    @Test
    fun yesButtonNavigatesToUserProfileCreationActivityWithIsShelterTrue() {
        onView(withId(R.id.yes_button)).perform(click())

        Intents.intended(
            allOf(
                hasComponent(UserProfileCreationActivity::class.java.name),
                hasExtra("is_shelter", true)
            )
        )
    }

    @Test
    fun noButtonNavigatesToUserProfileCreationActivityWithIsShelterFalse() {
        onView(withId(R.id.no_button)).perform(click())

        Intents.intended(
            allOf(
                hasComponent(UserProfileCreationActivity::class.java.name),
                hasExtra("is_shelter", false)
            )
        )
    }
}
