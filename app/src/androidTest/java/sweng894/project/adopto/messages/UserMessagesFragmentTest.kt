package sweng894.project.adopto.messages

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R
import java.util.concurrent.TimeoutException

@MediumTest
@RunWith(AndroidJUnit4::class)
class UserMessagesFragmentTest {

    @Test
    fun testFragmentLaunchesAndDisplaysComponents() {
        launchFragmentInContainer<UserMessagesFragment>(themeResId = R.style.Theme_Adopto)

        onView(withId(R.id.chat_list_recycler)).check(matches(isDisplayed()))
        onView(withId(R.id.message_recycler)).check(matches(isDisplayed()))
        onView(withId(R.id.message_input_field)).check(matches(isDisplayed()))
        onView(withId(R.id.send_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testSendButtonDisabledWhenEmpty() {
        launchFragmentInContainer<UserMessagesFragment>(themeResId = R.style.Theme_Adopto)
        onView(withId(R.id.message_input_field)).perform(clearText())
        onView(withId(R.id.send_button)).check(matches(not(isEnabled())))
    }


    @Test
    fun testToggleChatListButtonWorks() {
        launchFragmentInContainer<UserMessagesFragment>(themeResId = R.style.Theme_Adopto)
        onView(withId(R.id.expand_collapse_button)).perform(click())
        onView(withId(R.id.chat_list_recycler)).check(matches(isDisplayed()))
    }
}
