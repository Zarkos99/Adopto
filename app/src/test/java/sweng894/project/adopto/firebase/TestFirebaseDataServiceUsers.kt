package sweng894.project.adopto.firebase

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.User

class FirebaseDataServiceUsersTest {

    private lateinit var service: FirebaseDataServiceUsers

    // Mock Firebase Firestore
    private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
    private val mockDocumentRef: DocumentReference = mockk(relaxed = true)
    private val mockListenerRegistration: ListenerRegistration = mockk(relaxed = true)

    @Before
    fun setUp() {
        // Mock FirebaseApp initialization
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp
        every { FirebaseApp.getInstance() } returns mockFirebaseApp

        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        mockkObject(Strings)
        mockkStatic(Log::class)
        mockkStatic(::getCurrentUserId)
        every { getCurrentUserId() } returns "testUserId"

        every { Firebase.firestore } returns mockFirestore
        every { getCurrentUserId() } returns "testUserId"

        every {
            mockFirestore.collection("Users").document("testUserId")
        } returns mockDocumentRef
        val snapshotSlot = slot<EventListener<DocumentSnapshot>>()
        every { mockDocumentRef.addSnapshotListener(capture(snapshotSlot)) } answers {
            mockListenerRegistration
        }

        service = FirebaseDataServiceUsers()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onBindRegistersSnapshotListenerAndReturnsBinder() {
        val intent = mockk<Intent>(relaxed = true)
        val binder: IBinder = service.onBind(intent)

        verify { mockDocumentRef.addSnapshotListener(any()) }
        assert(binder is FirebaseDataServiceUsers.LocalBinder)
    }

    @Test
    fun onSnapshotListenerUpdatesUserDataWhenSnapshotExists() {
        val mockUser = User(user_id = "John Doe", is_shelter = false)
        val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject<User>() } returns mockUser

        val capturedListener = slot<EventListener<DocumentSnapshot>>()
        every { mockDocumentRef.addSnapshotListener(capture(capturedListener)) } answers {
            mockListenerRegistration
        }

        // Call onBind to trigger the snapshot listener setup
        service.onBind(intent)

        // Now verify that addSnapshotListener was called
        verify { mockDocumentRef.addSnapshotListener(any()) }

        // Simulate Firestore triggering the snapshot listener
        capturedListener.captured.onEvent(mockSnapshot, null)

        assert(service.current_user_data == mockUser)
    }

    @Test
    fun onSnapshotListenerDoesNotUpdateUserDataWhenSnapshotIsNull() {
        val intent = mockk<Intent>(relaxed = true)

        val capturedListener = slot<EventListener<DocumentSnapshot>>()
        every { mockDocumentRef.addSnapshotListener(capture(capturedListener)) } answers {
            mockListenerRegistration
        }

        // Call onBind to trigger the snapshot listener setup
        service.onBind(intent)

        // Verify that addSnapshotListener was called
        verify { mockDocumentRef.addSnapshotListener(any()) }

        // Simulate Firestore triggering the snapshot listener with a null snapshot
        capturedListener.captured.onEvent(null, null)

        // Verify that current_user_data remains null
        assert(service.current_user_data == null)
    }

    @Test
    fun onSnapshotListenerLogsErrorWhenExceptionOccurs() {
        val intent = mockk<Intent>(relaxed = true)
        val mockException = mockk<FirebaseFirestoreException>(relaxed = true)

        val capturedListener = slot<EventListener<DocumentSnapshot>>()
        every { mockDocumentRef.addSnapshotListener(capture(capturedListener)) } answers {
            mockListenerRegistration
        }

        mockkStatic(Log::class)
        every { Log.w(any(), any(), any()) } returns 0

        // Call onBind to trigger the snapshot listener setup
        service.onBind(intent)

        // Verify that addSnapshotListener was called
        verify { mockDocumentRef.addSnapshotListener(any()) }

        // Simulate Firestore triggering the snapshot listener with an exception
        capturedListener.captured.onEvent(null, mockException)

        // Verify that Log.w() was called with the expected error message
        verify { Log.w("Firebase Database", "Listen failed.", mockException) }
    }

    @Test
    fun registerCallbackAddsCallbackToList() {
        var is_called = false
        val callback: () -> Unit = { is_called = true }

        service.registerCallback(callback)

        service.callCallbacks()
        assertTrue(is_called)
    }

    @Test
    fun callCallbacksExecutesAllRegisteredCallbacks() {
        val callback1: () -> Unit = mockk(relaxed = true)
        val callback2: () -> Unit = mockk(relaxed = true)

        service.registerCallback(callback1)
        service.registerCallback(callback2)

        service.callCallbacks()

        verify(exactly = 1) { callback1.invoke() }
        verify(exactly = 1) { callback2.invoke() }
    }
}
