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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sweng894.project.adopto.data.User
import sweng894.project.adopto.Strings
import sweng894.project.adopto.App
import sweng894.project.adopto.TestApplication

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class, manifest = Config.NONE,
    sdk = [33] // Limit Robolectric to API 3
)
class FirebaseDataServiceUsersTest {

    private lateinit var service: FirebaseDataServiceUsers
    private val snapshotSlot = slot<EventListener<DocumentSnapshot>>() // Capture Firestore listener

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<TestApplication>()

        // Mock `App.instance`
        mockkObject(App)
        every { App.instance } returns context

        // Mock Strings.get() to prevent resource dependency errors
        mockkObject(Strings)
        every { Strings.get(any()) } returns "mocked_collection_name"

        // Mock Firebase Initialization
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp
        every { FirebaseApp.getInstance() } returns mockFirebaseApp

        // Mock FirebaseAuth
        mockkStatic(FirebaseAuth::class)
        val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
        every { mockFirebaseAuth.currentUser?.uid } returns "test_user_id"

        // Mock Firestore
        mockkStatic(FirebaseFirestore::class)
        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockDocumentReference = mockk<DocumentReference>(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocumentReference

        every { mockDocumentReference.addSnapshotListener(capture(snapshotSlot)) } answers {
            mockk() // Return a mock ListenerRegistration
        }

        // Initialize Firebase in Robolectric test environment
        FirebaseApp.initializeApp(context)

        // Initialize service
        service = FirebaseDataServiceUsers()
        service.onBind(Intent()) // Simulate service binding
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
