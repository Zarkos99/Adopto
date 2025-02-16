package sweng894.project.adopto.preferences

import android.content.Context
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import sweng894.project.adopto.TestApplication
import sweng894.project.adopto.R

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class, manifest = Config.NONE, sdk = [33])
class PreferencesActivityTest {

    private lateinit var scenario: ActivityScenario<PreferencesActivity>
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Ensure Firebase is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        // Mock FirebaseAuth and FirebaseUser
        mockkStatic(FirebaseAuth::class)
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.displayName } returns "Test User"
        every { mockUser.email } returns "test@example.com"

        // Mock Task<Void> for updateProfile()
        val mockTask = mockk<Task<Void>>(relaxed = true)
        every { mockTask.isSuccessful } returns true

        every { mockUser.updateProfile(any()) } answers {
            firstArg<UserProfileChangeRequest>()
            mockTask
        }
        every { mockTask.addOnCompleteListener(any()) } answers {
            val listener = firstArg<OnCompleteListener<Void>>()
            listener.onComplete(mockTask)
            mockTask
        }

        // Launch the activity
        scenario = ActivityScenario.launch(PreferencesActivity::class.java)
    }

    @Test
    fun testInitializeInputFields() {
        scenario.onActivity { activity ->
            activity.initializeInputFields(mockUser)

            val displayNameField = activity.findViewById<EditText>(R.id.display_name_input_field)
            val emailField = activity.findViewById<EditText>(R.id.email_input_field)

            assertEquals("Test User", displayNameField.text.toString())
            assertEquals("test@example.com", emailField.text.toString())
        }
    }

    @Test
    fun testCalculateSaveButtonClickability() {
        scenario.onActivity { activity ->
            val saveButton = activity.findViewById<Button>(R.id.save_preferences_button)

            assertFalse(saveButton.isEnabled)

            activity.calculateSaveButtonClickability(mockUser, "new_email@example.com", "New Name")
            assertTrue(saveButton.isEnabled)

            activity.calculateSaveButtonClickability(mockUser, "test@example.com", "New Name")
            assertTrue(saveButton.isEnabled)

            activity.calculateSaveButtonClickability(mockUser, "test@example.com", "Test User")
            assertFalse(saveButton.isEnabled)
        }
    }

    @Test
    fun testEnableDisableButton() {
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.save_preferences_button)

            activity.disableButton(button)
            assertFalse(button.isEnabled)
            assertFalse(button.isClickable)

            activity.enableButton(button)
            assertTrue(button.isEnabled)
            assertTrue(button.isClickable)

            activity.disableButton(button)
            assertFalse(button.isEnabled)
            assertFalse(button.isClickable)
        }
    }

    @Test
    fun testSavePreferencesButtonUpdatesUserProfile() {
        scenario.onActivity { activity ->
            val saveButton = activity.findViewById<Button>(R.id.save_preferences_button)

            activity.m_display_name_input_field.setText("New Name")
            activity.m_display_name_input_field.clearFocus()

            activity.m_email_input_field.setText("new_email@example.com")
            activity.m_email_input_field.clearFocus()

            println("Display name before update: ${mockUser.displayName}")
            println("Email before update: ${mockUser.email}")

            assertTrue("Save button should be enabled before clicking", saveButton.isEnabled)

            saveButton.performClick()

            shadowOf(Looper.getMainLooper()).idle()

            println("Save button clicked.")

            val captor = slot<UserProfileChangeRequest>()
            verify { mockUser.updateProfile(capture(captor)) }

            println("Captured display name update: ${captor.captured.displayName}")
            assertEquals("New Name", captor.captured.displayName)

            verify { mockUser.verifyBeforeUpdateEmail("new_email@example.com") }

            assertFalse(saveButton.isEnabled)
        }
    }

    @Test
    fun testLogoutButtonLogsOutUser() {
        scenario.onActivity { activity ->
            val logoutButton = activity.findViewById<Button>(R.id.logout_button)

            println("MockAuth instance in test: $mockAuth")

            assertTrue("Logout button should be clickable", logoutButton.isClickable)

            logoutButton.performClick()

            shadowOf(Looper.getMainLooper()).idle()

            println("Logout button clicked.")

            verify { mockAuth.signOut() }

            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun testEmailInputTriggersSaveButton() {
        scenario.onActivity { activity ->
            val saveButton = activity.findViewById<Button>(R.id.save_preferences_button)

            assertFalse(saveButton.isEnabled)

            activity.m_email_input_field.setText("new_email@example.com")

            assertTrue(saveButton.isEnabled)
        }
    }
}
