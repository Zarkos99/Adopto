package sweng894.project.adopto.authentication

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
@LargeTest
class UserProfileCreationActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<UserProfileCreationActivity>(
        Intent(Intent.ACTION_MAIN).apply {
            setClassName(
                "sweng894.project.adopto",
                "sweng894.project.adopto.authentication.UserProfileCreationActivity"
            )
            putExtra("is_shelter", true) // mock the boolean input
        }
    )

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun viewsAreVisible() {
        onView(withId(R.id.user_description_input)).check(matches(isDisplayed()))
        onView(withId(R.id.user_location_input)).check(matches(isDisplayed()))
        onView(withId(R.id.done_button)).check(matches(isDisplayed()))
    }
}
