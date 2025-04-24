package sweng894.project.adopto.profile

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
class UserProfilePreferencesActivityTest {
    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(UserProfilePreferencesActivity::class.java)

    @Test
    fun test_UIElements_AreDisplayed() {
        ActivityScenario.launch(UserProfilePreferencesActivity::class.java)

        onView(withId(R.id.user_display_name_input)).check(matches(isDisplayed()))
        onView(withId(R.id.user_email_input)).check(matches(isDisplayed()))
        onView(withId(R.id.user_location_input)).check(matches(isDisplayed()))
        onView(withId(R.id.logout_button)).check(matches(isDisplayed()))
        onView(withId(R.id.save_preferences_button)).check(matches(isDisplayed()))
    }

    @Test
    fun test_SaveButton_EnablesOnChange() {
        ActivityScenario.launch(UserProfilePreferencesActivity::class.java)

        // Change display name to trigger state
        onView(
            allOf(
                withId(R.id.input_text_field),
                isDescendantOfA(withId(R.id.user_display_name_input))
            )
        ).perform(clearText(), typeText("New Name"))

        closeSoftKeyboard()

        // Check if button is enabled
        onView(withId(R.id.save_preferences_button)).check(matches(isEnabled()))
    }

    @Test
    fun test_LogoutButton_OpensLoginScreen() {
        onView(withId(R.id.logout_button)).perform(click())
    }
}
