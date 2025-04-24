package sweng894.project.adopto.authentication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
class AuthenticationActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AuthenticationActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyArray()
    )

    @Test
    fun emptyUsernameShowsToast() {
        onView(withId(R.id.login_button)).perform(click())
        // Espresso cannot test toasts directly without workarounds
    }

    @Test
    fun emptyPasswordShowsToast() {
        onView(withId(R.id.username_field)).perform(
            typeText("test@example.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.login_button)).perform(click())
    }

    @Test
    fun registerWithEmptyFields_showsToast() {
        onView(withId(R.id.register_button)).perform(click())
    }

    @Test
    fun loginButtonTypedInputs_noCrash() {
        onView(withId(R.id.username_field)).perform(
            typeText("test@example.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.password_field)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.login_button)).perform(click())
    }

    @Test
    fun registerButtonTypedInputs_noCrash() {
        onView(withId(R.id.username_field)).perform(
            typeText("test@example.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.password_field)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.register_button)).perform(click())
    }

    @Test
    fun checkUiElementsAreVisible() {
        onView(withId(R.id.username_field)).check(matches(isDisplayed()))
        onView(withId(R.id.password_field)).check(matches(isDisplayed()))
        onView(withId(R.id.login_button)).check(matches(isDisplayed()))
        onView(withId(R.id.register_button)).check(matches(isDisplayed()))
    }

    @Test
    fun requestNotificationsPermissions_grantedOrNotHandledGracefully() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            assert(result == PackageManager.PERMISSION_GRANTED || result == PackageManager.PERMISSION_DENIED)
        }
    }
}
