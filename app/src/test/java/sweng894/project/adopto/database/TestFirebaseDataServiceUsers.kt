package sweng894.project.adopto.database

import android.content.Intent
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.data.User
import sweng894.project.adopto.Strings
import sweng894.project.adopto.App

class FirebaseDataServiceUsersTest {

    private lateinit var service: FirebaseDataServiceUsers
    private val snapshotSlot = slot<EventListener<DocumentSnapshot>>() // Capture Firestore listener

    @Before
    fun setUp() {
    }

    @Test
    fun testServiceBindsCorrectly() {
        val binder: IBinder = service.onBind(Intent())
        assertNotNull(binder)
        assertTrue(binder is FirebaseDataServiceUsers.LocalBinder)
    }

    @Test
    fun testCallbackRegistrationAndExecution() {
        var callbackExecuted = false

        // Register a callback
        service.registerCallback {
            callbackExecuted = true
        }

        // Simulate data change
        service.callCallbacks()

        assertTrue("Callback should have been executed", callbackExecuted)
    }

    @Test
    fun testFirebaseSnapshotListenerUpdatesUserData() {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val testUser = User(user_id = "test123", is_shelter = true)

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(User::class.java) } returns testUser

        // Manually invoke the captured snapshot listener
        snapshotSlot.captured.onEvent(mockSnapshot, null)

        assertNotNull(service.current_user_data)
        assertEquals("test123", service.current_user_data?.user_id)
        assertTrue(service.current_user_data?.is_shelter == true)
    }

    @Test
    fun testFirebaseSnapshotListenerHandlesNullData() {
        // Simulate null data event
        snapshotSlot.captured.onEvent(null, null)

        // ✅ UPDATED: `current_user_data` should now be NULL instead of a default `User()`
        assertNull(
            "current_user_data should remain null when snapshot is null",
            service.current_user_data
        )
    }

    @Test
    fun testFirebaseSnapshotListenerHandlesFirestoreRrror() {
        val mockException = FirebaseFirestoreException(
            "Firestore Error",
            FirebaseFirestoreException.Code.UNKNOWN
        )

        // Simulate an error in Firestore listener
        snapshotSlot.captured.onEvent(null, mockException)

        assertNull(service.current_user_data) // User data should not update on failure
    }
}
