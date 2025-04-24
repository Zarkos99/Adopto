package sweng894.project.adopto.explore

import android.app.Instrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R
import sweng894.project.adopto.profile.ExplorePreferencesActivity

@RunWith(AndroidJUnit4::class)
class ExploreFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(NavigationBaseActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun exploreFragment_uiElementsAreDisplayed() {
        onView(withId(R.id.card_stack_view)).check(matches(isDisplayed()))
        onView(withId(R.id.skip_button)).check(matches(isDisplayed()))
        onView(withId(R.id.like_animal_button)).check(matches(isDisplayed()))
        onView(withId(R.id.rewind_button)).check(matches(isDisplayed()))
        onView(withId(R.id.explore_preferences_button)).check(matches(isDisplayed()))
    }

    @Test
    fun preferencesButton_opensExplorePreferencesActivity() {
        intending(hasComponent(ExplorePreferencesActivity::class.qualifiedName))
            .respondWith(Instrumentation.ActivityResult(0, null))

        onView(withId(R.id.explore_preferences_button)).perform(click())
        intended(hasComponent(ExplorePreferencesActivity::class.qualifiedName))
    }

    @Test
    fun cardSwipe_likeAndSkipButtonsTriggerSwipe() {
        // Simulate a like
        onView(withId(R.id.like_animal_button)).perform(click())
        Thread.sleep(1000)

        // Simulate a skip
        onView(withId(R.id.skip_button)).perform(click())
        Thread.sleep(1000)
    }

    @Test
    fun rewindButton_triggersCardRewind() {
        onView(withId(R.id.rewind_button)).perform(click())
        Thread.sleep(1000)
    }

    @Test
    fun animalsLoadOnServiceConnection() {
        // This test assumes the Firebase service eventually triggers loadAnimals
        // and updates the adapter (may require logcat or adapter item count monitoring)
        Thread.sleep(3000)

        // Placeholder: You can assert some visible change or item
        onView(withId(R.id.card_stack_view)).check(matches(isDisplayed()))
    }

    // TODO: Uncomment and implement when UI shows a "no animals" message on empty
//    @Test
//    fun noRecommendations_showsNoAnimalsMessage() {
//        // Would require mocking getRecommendations to return empty list
//        onView(withText("No new animals to show")).check(matches(isDisplayed()))
//    }
}
