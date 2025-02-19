//package sweng894.project.adopto.authentication
//
//import android.os.Build
//import android.widget.Button
//import android.widget.EditText
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import androidx.test.core.app.ActivityScenario
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.AuthResult
//import com.google.android.gms.tasks.Task
//import com.google.android.gms.tasks.Tasks
//import io.mockk.*
//import kotlinx.coroutines.test.runTest
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import org.robolectric.shadows.ShadowToast
//import sweng894.project.adopto.R
//
//@RunWith(RobolectricTestRunner::class)
//@Config(
//    sdk = [Build.VERSION_CODES.P],
//    manifest = Config.NONE,
//    application = TestApplication::class
//) // 👈 Prevents Robolectric from looking for `AndroidManifest.xml`
//class AuthenticationActivityTest {
//
//    @get:Rule
//    val instantTaskExecutorRule = InstantTaskExecutorRule()
//
//    private lateinit var firebaseAuth: FirebaseAuth
//
//    // Helper function to return a mock Task<AuthResult>
//    private fun mockTask(success: Boolean): Task<AuthResult> {
//        return if (success) {
//            Tasks.forResult(mockk(relaxed = true)) // Simulate success
//        } else {
//            Tasks.forException(Exception("Authentication failed")) // Simulate failure
//        }
//    }
//
//    @Before
//    fun setUp() {
//        firebaseAuth = mockk(relaxed = true)
//    }
//
//    @Test
//    fun `test login with empty username shows toast`() {
//        val scenario = ActivityScenario.launch(AuthenticationActivity::class.java)
//
//        scenario.onActivity { activity ->
//            val loginButton = activity.findViewById<Button>(R.id.login_button)
//            val usernameField = activity.findViewById<EditText>(R.id.username_field)
//            val passwordField = activity.findViewById<EditText>(R.id.password_field)
//
//            usernameField.setText("")
//            passwordField.setText("password123")
//
//            loginButton.performClick()
//
//            val latestToast = ShadowToast.getTextOfLatestToast()
//            assert(latestToast == "Please input your username")
//        }
//    }
//
//    @Test
//    fun `test successful login calls FirebaseAuth`() = runTest {
//        val scenario = ActivityScenario.launch(AuthenticationActivity::class.java)
//
//        scenario.onActivity { activity ->
//            val loginButton = activity.findViewById<Button>(R.id.login_button)
//            val usernameField = activity.findViewById<EditText>(R.id.username_field)
//            val passwordField = activity.findViewById<EditText>(R.id.password_field)
//
//            usernameField.setText("test@example.com")
//            passwordField.setText("password123")
//
//            every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns mockTask(true)
//
//            loginButton.performClick()
//
//            verify { firebaseAuth.signInWithEmailAndPassword("test@example.com", "password123") }
//        }
//    }
//}
