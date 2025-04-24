package sweng894.project.adopto

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationBaseActivityTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(NavigationBaseActivity::class.java)

    @Test
    fun testActivityLaunchesSuccessfully() {
        ActivityScenario.launch(NavigationBaseActivity::class.java)
        onView(withId(R.id.nav_view)).check(matches(isDisplayed()))
    }

    @Test
    fun testInitialTabNavigationExplore() {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NavigationBaseActivity::class.java
        ).apply {
            putExtra("initial_tab", R.id.navigation_explore)
        }

        ActivityScenario.launch<NavigationBaseActivity>(intent).use {
            onView(withId(R.id.navigation_explore)).check(matches(isSelected()))
        }
    }

    @Test
    fun testInitialTabNavigationMessages() {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NavigationBaseActivity::class.java
        ).apply {
            putExtra("initial_tab", R.id.navigation_messages)
        }

        ActivityScenario.launch<NavigationBaseActivity>(intent).use {
            onView(withId(R.id.navigation_messages)).check(matches(isSelected()))
        }
    }

    @Test
    fun testHandleIntentOpenChatIdTriggers() {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NavigationBaseActivity::class.java
        ).apply {
            putExtra("initial_tab", R.id.navigation_messages)
            putExtra("open_chat_id", "chat123")
        }

        ActivityScenario.launch<NavigationBaseActivity>(intent).use {
            // You might want to add IdlingResource or delay check here.
            // For now, verify nav view still loads
            onView(withId(R.id.nav_view)).check(matches(isDisplayed()))
        }
    }
}
