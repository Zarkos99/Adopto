package sweng894.project.adopto.profile

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
@LargeTest
class UserProfileViewingActivityTest {

    @Test
    fun test_UserProfileViewing_DisplaysCorrectViews() {
        // Setup: Launch activity with a valid mock user ID
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserProfileViewingActivity::class.java
        ).apply {
            putExtra(
                "user_id",
                "mock_user_123"
            ) // Use a test user ID that your test Firebase data can respond to
        }

        ActivityScenario.launch<UserProfileViewingActivity>(intent)

        // Assert views are visible
        onView(withId(R.id.profile_picture_view)).check(matches(isDisplayed()))
        onView(withId(R.id.public_username)).check(matches(isDisplayed()))
        onView(withId(R.id.biography_field)).check(matches(isDisplayed()))
        onView(withId(R.id.chat_button)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()))
    }
}
