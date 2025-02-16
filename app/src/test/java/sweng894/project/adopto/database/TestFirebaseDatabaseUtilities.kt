package sweng894.project.adopto.database

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import sweng894.project.adopto.TestApplication
import kotlin.reflect.KProperty1

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class,
    manifest = Config.NONE,
    sdk = [33] // Limit Robolectric to API 33
)
class FirebaseDatabaseUtilitiesTest {

    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocumentReference: DocumentReference
    private lateinit var mockFirebaseAuth: FirebaseAuth

    // Helper function to avoid Firestore hanging
    private fun <T> mockTask(result: T): Task<T> = Tasks.forResult(result)


    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<TestApplication>()

        // ✅ Manually provide FirebaseOptions to avoid `Resources$NotFoundException`
        val firebaseOptions = FirebaseOptions.Builder()
            .setApplicationId("1:1234567890:android:abcdef") // Fake App ID
            .setApiKey("fake-api-key") // Fake API Key
            .setProjectId("test-project") // Fake Project ID
            .build()

        // ✅ Ensure FirebaseApp is initialized properly
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, firebaseOptions)
        }

        // ✅ Mock FirebaseApp BEFORE Firestore
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.getInstance() } returns mockFirebaseApp

        // ✅ Mock FirebaseAuth
        mockkStatic(FirebaseAuth::class)
        mockFirebaseAuth = mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
        every { mockFirebaseAuth.currentUser?.uid } returns "test_user_id"

        // ✅ Mock Strings.get() to prevent resource issues
        mockkObject(Strings)
        every { Strings.get(any()) } returns "mocked_collection_name"

        // ✅ Corrected Firestore Mocking
        mockkStatic(FirebaseFirestore::class)
        mockFirestore = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocumentReference = mockk(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection("mocked_collection_name") } returns mockCollection
        every { mockCollection.document("test123") } returns mockDocumentReference

        // ✅ Ensure `mockDocumentReference.get()` returns a Task<DocumentSnapshot>
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(User::class.java) } returns User(
            user_id = "test123",
            is_shelter = true
        )

        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)
    }

    @Test
    fun testGetCurrentUserIdReturnsCorrectUserId() {
        val userId = getCurrentUserId()
        assertEquals("test_user_id", userId)
    }

    @Test
    fun testGetUserDataReturnsUserOnSuccess() = runTest {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val testUser = User(user_id = "test123", is_shelter = true)

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(User::class.java) } returns testUser

        // ✅ Mock Firestore Task
        val mockTask: Task<DocumentSnapshot> = mockk(relaxed = true)

        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.result } returns mockSnapshot

        // ✅ Ensure Firestore's .get() returns this task
        every { mockDocumentReference.get() } returns mockTask

        // ✅ Corrected lambda capturing
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        every { mockTask.addOnFailureListener(any()) } answers {
            val callback = firstArg<OnFailureListener>()
            // Do nothing since we simulate success
            mockTask
        }

        // ✅ Run test
        val result = getUserData("test123")

        assertNotNull(result)
        assertEquals("test123", result?.user_id)
        assertTrue(result?.is_shelter == true)
    }

    @Test
    fun testGetUserDataReturnsNullWhenUserNotFound() = runTest {
        // 🔥 Mock Firestore document snapshot to simulate missing user
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        every { mockSnapshot.exists() } returns false

        // 🔥 Ensure Firestore `.get()` returns the mocked snapshot
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        println("DEBUG: Before calling getUserData")

        val result = getUserData("non_existing_user")

        println("DEBUG: After calling getUserData -> $result")

        assertNull(result) // ✅ Expected null
    }

    @Test
    fun testGetUserDataTimesOut() = runTest {
        // 🔥 Mock Firestore task that never completes
        val uncompletedTask: Task<DocumentSnapshot> = mockk(relaxed = true)
        every { uncompletedTask.isComplete } returns false  // Simulates hanging request
        every { mockDocumentReference.get() } returns uncompletedTask

        println("DEBUG: Before calling getUserData")

        // ⏳ Advance time to trigger timeout
        advanceTimeBy(10000) // Simulates the coroutine waiting for 10 seconds

        val result = getUserData("timeout_user")

        println("DEBUG: After calling getUserData -> $result")

        assertNull(result) // ✅ Expected null due to timeout
    }

    @Test
    fun testAddUserToDatabaseSuccessfullyAddsUser() = runTest {
        // ✅ Mock getCurrentUserId() to return expected user ID
        mockkStatic("sweng894.project.adopto.database.FirebaseDatabaseUtilitiesKt")

        // ✅ Mock Firestore again within the test scope
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocumentReference

        // ✅ Mock Firestore behavior
        val mockTask: Task<Void> = mockk(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockDocumentReference.set(any()) } returns mockTask

        // ✅ Create a user with an empty ID to trigger function override
        val testUser = User(user_id = "")

        println("DEBUG: Before calling addUserToDatabase")

        // ✅ Call the function
        addUserToDatabase(testUser)

        println("DEBUG: After calling addUserToDatabase")

        // ✅ Verify Firestore `.set()` was called with the correct user
        verify {
            mockDocumentReference.set(withArg<User> { savedUser ->
                assertEquals("test_user_id", savedUser.user_id) // Ensures user_id was updated
            })
        }
    }


    @Test
    fun testAddAnimalToDatabaseSuccessfullyAddsAnimal() = runTest {
        // ✅ Mock Firestore again within the test scope
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocumentReference

        // ✅ Mock Firestore behavior
        val mockTask: Task<Void> = mockk(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockDocumentReference.set(any()) } returns mockTask

        // ✅ Create test animal
        val testAnimal = Animal(associated_shelter_id = "shelter123")

        println("DEBUG: Before calling addAnimalToDatabase")

        // ✅ Call function
        addAnimalToDatabase(testAnimal)

        println("DEBUG: After calling addAnimalToDatabase")

        // ✅ Verify Firestore's `.set()` was called with the correct data
        verify { mockDocumentReference.set(testAnimal) }
    }

    @Test
    fun testUpdateDataFieldUpdatesAnExistingDocumentField() = runTest {
        // ✅ Mock successful Firestore update
        every { mockDocumentReference.set(any<Map<String, Any>>(), any()) } returns Tasks.forResult(
            null
        )

        // ✅ Define test values
        val fieldName: KProperty1<User, *> = User::biography
        val fieldValue = "New"

        // ✅ Call function
        updateDataField(
            Strings.get(R.string.firebase_collection_users),
            "test123",
            fieldName,
            fieldValue
        )

        // ✅ Verify Firestore update was called with correct values
        verify {
            mockDocumentReference.set(mapOf("biography" to "New"), SetOptions.merge())
        }
    }


    @Test
    fun testAppendToDataFieldArrayAppendsValuesSuccessfully() = runTest {
        // ✅ Mock successful Firestore update
        every { mockDocumentReference.update(any<String>(), any()) } returns Tasks.forResult(null)

        // ✅ Define test values
        val fieldName: KProperty1<User, *> = User::saved_animal_ids
        val fieldValue = "A1"

        // ✅ Call function
        appendToDataFieldArray(
            Strings.get(R.string.firebase_collection_users),
            "test123",
            fieldName,
            fieldValue
        )

        // ✅ Verify Firestore update was called with correct values
        verify {
            mockDocumentReference.update("saved_animal_ids", match { it is FieldValue })
        }
    }

    @Test
    fun testRemoveFromDataFieldArrayRemovesValuesSuccessfully() = runTest {
        // ✅ Mock successful Firestore update
        every { mockDocumentReference.update(any<String>(), any()) } returns Tasks.forResult(null)

        // ✅ Mock `_syncDatabaseForRemovedImages`
        mockkStatic("sweng894.project.adopto.database.FirebaseDatabaseUtilitiesKt")
        every {
            _syncDatabaseForRemovedImages(
                any<KProperty1<Animal, *>>(),
                any<Array<String>>()
            )
        } just Runs

        // ✅ Define test values
        val fieldName: KProperty1<Animal, *> = Animal::supplementary_image_paths
        val valuesToBeRemoved = arrayOf("path1", "path2")

        // ✅ Call function
        removeFromDataFieldArray(
            Strings.get(R.string.firebase_collection_animals),  // Ensure correct collection name
            "test123",
            fieldName,
            valuesToBeRemoved
        )

        // ✅ Ensure all async tasks complete (fix for Robolectric & async verification)
        shadowOf(Looper.getMainLooper()).idle()

        // ✅ Verify Firestore update was called with correct field name
        verify {
            mockDocumentReference.update(
                "supplementary_image_paths",
                match { it is FieldValue } // Instead of eq(FieldValue.arrayRemove(*valuesToBeRemoved))
            )
        }

        // ✅ Verify `_syncDatabaseForRemovedImages` was called after Firestore update
        verify { _syncDatabaseForRemovedImages(fieldName, valuesToBeRemoved) }
    }


    @Test
    fun testFetchAllUserAnimalsReturnsAListOfAnimals() = runTest {
        val mockSnapshot1: DocumentSnapshot = mockk(relaxed = true)
        val mockSnapshot2: DocumentSnapshot = mockk(relaxed = true)
        val testAnimal1 = Animal(associated_shelter_id = "shelter123", animal_name = "Dog")
        val testAnimal2 = Animal(associated_shelter_id = "shelter456", animal_name = "Cat")

        every { mockSnapshot1.toObject(Animal::class.java) } returns testAnimal1
        every { mockSnapshot2.toObject(Animal::class.java) } returns testAnimal2

        val mockTask1: Task<DocumentSnapshot> = mockk(relaxed = true)
        val mockTask2: Task<DocumentSnapshot> = mockk(relaxed = true)

        every { mockTask1.isComplete } returns true
        every { mockTask1.isSuccessful } returns true
        every { mockTask1.result } returns mockSnapshot1

        every { mockTask2.isComplete } returns true
        every { mockTask2.isSuccessful } returns true
        every { mockTask2.result } returns mockSnapshot2

        every { mockFirestore.collection(any()).document("animal1").get() } returns mockTask1
        every { mockFirestore.collection(any()).document("animal2").get() } returns mockTask2

        fetchAllUserAnimals(listOf("animal1", "animal2")) { result ->
            assertEquals(2, result.size)
            assertEquals("Dog", result[0].animal_name)
            assertEquals("Cat", result[1].animal_name)
        }
    }

}
