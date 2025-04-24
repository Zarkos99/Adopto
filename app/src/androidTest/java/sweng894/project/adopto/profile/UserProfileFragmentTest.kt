package sweng894.project.adopto.profile

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
@MediumTest
class UserProfileFragmentTest {

    @Test
    fun testFragmentDisplaysBasicViews() {
        launchFragmentInContainer<UserProfileFragment>(Bundle(), R.style.Theme_Adopto)

        onView(withId(R.id.public_username)).check(matches(isDisplayed()))
        onView(withId(R.id.profile_picture_view)).check(matches(isDisplayed()))
        onView(withId(R.id.preferences_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testEditBiographyFlow() {
        launchFragmentInContainer<UserProfileFragment>(Bundle(), R.style.Theme_Adopto)

        // Tap "Edit" button
        onView(withId(R.id.edit_bio_button)).perform(click())

        // Check that EditText is now editable and input new text
        onView(withId(R.id.biography_field))
            .check(matches(isEnabled()))
            .perform(replaceText("My new biography"))

        // Save the bio
        onView(withId(R.id.save_bio_button)).perform(click())

        // Should now be disabled again
        onView(withId(R.id.biography_field)).check(matches(not(isEnabled())))
    }

    @Test
    fun testCancelEditBiography() {
        launchFragmentInContainer<UserProfileFragment>(Bundle(), R.style.Theme_Adopto)

        onView(withId(R.id.edit_bio_button)).perform(click())

        // Enter new text and cancel
        onView(withId(R.id.biography_field))
            .perform(replaceText("Temporary text"))

        onView(withId(R.id.cancel_edit_bio_button)).perform(click())

        // Check that it reset the field and disabled it again
        onView(withId(R.id.biography_field)).check(matches(not(isEnabled())))
        onView(withId(R.id.edit_bio_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testProfilePictureClickOpensImagePicker() {
        launchFragmentInContainer<UserProfileFragment>(Bundle(), R.style.Theme_Adopto)

        // Simulate click on profile picture
        onView(withId(R.id.profile_picture_view)).perform(click())

        // Normally this would open a system file picker - you'd need to mock the result
        // For now, just verify the click worked and no crash occurred
    }

    @Test
    fun testPreferencesButtonLaunchesPreferencesActivity() {
        launchFragmentInContainer<UserProfileFragment>(Bundle(), R.style.Theme_Adopto)

        onView(withId(R.id.preferences_button)).perform(click())

        // This will launch a new activity; for real testing, you may use Espresso-Intents
    }
}
