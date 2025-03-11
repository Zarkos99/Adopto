package sweng894.project.adopto.database

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.App
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty1

class FirebaseDatabaseUtilitiesTest {

    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocumentReference: DocumentReference
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Before
    fun setUp() {
        // Mock Firebase Components
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        // Mock FirebaseStorage to isolate it from tests
        mockkStatic(FirebaseStorage::class)
        every { FirebaseStorage.getInstance() } returns mockk(relaxed = true)

        // Initialize Firebase Firestore
        mockFirestore = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocumentReference = mockk(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocumentReference
        // Mock Strings.get() to prevent resource dependency errors
        mockkObject(Strings)
        every { Strings.get(any()) } returns "mocked_collection_name"

        // Ensure Firestore `.set()` and `.get()` return results
        every { mockDocumentReference.set(any()) } returns Tasks.forResult(null)
        every { mockDocumentReference.get() } returns Tasks.forResult(mockk(relaxed = true))
        every { mockDocumentReference.update(any<String>(), any()) } returns Tasks.forResult(null)
        every { mockDocumentReference.delete() } returns Tasks.forResult(null)

        // Mock FirebaseAuth
        mockFirebaseAuth = mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
        every { mockFirebaseAuth.currentUser?.uid } returns "test_user_id"

        // Mock App instance to avoid "App.instance is not initialized" error
        mockkObject(App)
        every { App.isInitialized() } returns true
        every { App.instance } returns mockk(relaxed = true)

        // Mock Task execution to avoid calling `TaskExecutors.MAIN_THREAD`
        mockkStatic(Tasks::class)
        every { Tasks.forResult(null) } returns mockk(relaxed = true)
    }

    @Test
    fun testGetCurrentUserIdReturnsCorrectUserID() {
        val userId = getCurrentUserId()
        assertEquals("test_user_id", userId)
    }

    @Test
    fun testGetUserDataReturnsUserOnSuccess() = runBlocking {
        // Mock Firestore document snapshot
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val testUser = User(user_id = "test123", is_shelter = true)

        // Simulate a successful Firestore query
        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(User::class.java) } returns testUser

        // Ensure Firestore returns a properly completed Task
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        // Call the actual function
        val result = getUserData("test123")

        // Ensure the result is not null and contains the correct data
        assertNotNull(result)
        assertEquals("test123", result?.user_id)
        assertTrue(result?.is_shelter == true)

        // Verify Firestore `.get()` was actually called once
        verify(exactly = 1) { mockDocumentReference.get() }
    }

    @Test
    fun testGetUserDataReturnsNullWhenUserNotFound() = runBlocking {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        every { mockSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = getUserData("non_existing_user")

        assertNull(result)
    }

    @Test
    fun testGetUserDataAsynchronouslySuccessfullyRetrievesUser() {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val testUser = User(user_id = "test123", is_shelter = true)
        val mockTask: Task<DocumentSnapshot> = mockk(relaxed = true)

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(User::class.java) } returns testUser
        every { mockDocumentReference.get() } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockSnapshot)
            mockTask
        }

        val latch = CountDownLatch(1) // Ensures the test waits for Firestore
        var result: User?

        println("Before callback")
        getUserData("test123") { user ->
            println("In callback. User data retrieved: $user")
            result = user

            assertNotNull(result)
            assertEquals("test123", result?.user_id)
            assertTrue(result?.is_shelter == true)

            latch.countDown() // Signal that Firestore callback finished
        }

        assertTrue("Callback was not invoked in time", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testGetUserDataAsynchronouslyReturnsNullWhenUserFoundNull() {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val mockTask: Task<DocumentSnapshot> = mockk(relaxed = true)

        every { mockSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockSnapshot)
            mockTask
        }

        val latch = CountDownLatch(1)
        var result: User?

        getUserData("non_existing_user") { user ->
            result = user
            latch.countDown()

            assertNull(result)
        }

        assertTrue("Callback was not invoked in time", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testGetAnimalDataReturnsAnimalOnSuccess() = runBlocking {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val testAnimal = Animal(animal_id = "animal123", associated_shelter_id = "shelter123")

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.toObject(Animal::class.java) } returns testAnimal
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = getAnimalData("animal123")

        assertNotNull("Animal should not be null", result)
        assertEquals("animal123", result!!.animal_id)
        assertEquals("shelter123", result.associated_shelter_id)
    }

    @Test
    fun testGetAnimalDataReturnsNullWhenDocumentDoesNotExist() = runBlocking {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)

        every { mockSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = getAnimalData("non_existing_animal")

        assertNull("Should return null if document does not exist", result)
    }

    @Test
    fun testGetAnimalDataHandlesFirestoreFailure() = runBlocking {
        every { mockDocumentReference.get() } returns Tasks.forException(RuntimeException("Firestore error"))

        val result = getAnimalData("failing_animal")

        assertNull("Should return null if Firestore query fails", result)
    }

    @Test
    fun testGetAnimalDataHandlesTimeout() = runBlocking {
        val result = withTimeoutOrNull(100) { getAnimalData("slow_animal") }

        assertNull("Should return null if Firestore query times out", result)
    }

    @Test
    fun testAddUserToDatabaseSuccessfullyAddsUser() = runBlocking {
        val mockTask: Task<Void> = mockk(relaxed = true)
        every { mockTask.isSuccessful } returns true
        every { mockDocumentReference.set(any()) } returns mockTask

        val testUser = User(user_id = "")

        addUserToDatabase(testUser)

        verify(exactly = 1) { mockDocumentReference.set(any<User>()) }
    }

    @Test
    fun testAddAnimalToDatabaseSuccessfullyAddsAnimal() = runBlocking {
        val mockTask: Task<Void> = mockk(relaxed = true)
        every { mockTask.isSuccessful } returns true
        every { mockDocumentReference.set(any()) } returns mockTask

        val testAnimal = Animal(associated_shelter_id = "shelter123")

        addAnimalToDatabase(testAnimal)

        verify(exactly = 1) { mockDocumentReference.set(any<Animal>()) }
    }

    @Test
    fun testUpdateDataFieldUpdatesAnExistingDocumentField() = runBlocking {
        every { mockDocumentReference.set(any<Map<String, Any>>(), any()) } returns Tasks.forResult(
            null
        )

        val fieldName: KProperty1<User, *> = User::biography
        val fieldValue = "Updated Bio"

        updateDataField("mocked_collection_name", "test123", fieldName, fieldValue)

        verify(exactly = 1) {
            mockDocumentReference.set(mapOf("biography" to "Updated Bio"), SetOptions.merge())
        }
    }

    @Test
    fun testAppendToDataFieldArrayAppendsValuesSuccessfully() = runBlocking {
        every { mockDocumentReference.update(any<String>(), any()) } returns Tasks.forResult(null)

        val fieldName: KProperty1<User, *> = User::saved_animal_ids
        val fieldValue = "A1"

        appendToDataFieldArray("mocked_collection_name", "test123", fieldName, fieldValue)

        verify(exactly = 1) {
            mockDocumentReference.update(
                "saved_animal_ids",
                match { it is FieldValue })
        }
    }

    @Test
    fun testFetchAnimalsReturnsEmptyWhenNoAnimalIdsProvided() {
        val latch = CountDownLatch(1)
        var result: List<Animal>? = null

        fetchAnimals(emptyList()) { animals ->
            result = animals
            latch.countDown()
        }

        assertTrue("Callback was not invoked in time", latch.await(5, TimeUnit.SECONDS))
        assertNotNull(result)
        assertTrue(
            "Result should be an empty list when no animal IDs are provided",
            result!!.isEmpty()
        )
    }

    @Test
    fun testFetchAllAnimalsReturnsList() = runBlocking {
        val mockSnapshot: QuerySnapshot = mockk(relaxed = true)
        val mockQueryDocumentSnapshot: QueryDocumentSnapshot = mockk(relaxed = true)
        val mockTask: Task<QuerySnapshot> = mockk(relaxed = true)
        val testAnimal = Animal(associated_shelter_id = "shelter123")

        every { mockSnapshot.documents } returns listOf(mockQueryDocumentSnapshot)
        every { mockSnapshot.iterator() } returns listOf(mockQueryDocumentSnapshot).iterator() as MutableIterator<QueryDocumentSnapshot>
        every { mockQueryDocumentSnapshot.toObject(Animal::class.java) } returns testAnimal
        every { mockCollection.get() } returns mockTask
        // Manually trigger the Firestore success callback
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockSnapshot)
            mockTask
        }

        val latch = CountDownLatch(1)
        var result: List<Animal>?

        fetchAllAnimals { animals ->
            result = animals

            assertNotNull(result)
            assertEquals(1, result!!.size)
            assertEquals("shelter123", result!![0].associated_shelter_id)

            latch.countDown()
        }

        assertTrue("Callback was not invoked in time", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testRemoveAnimalFromDatabaseSuccessfullyRemovesAnimal() = runBlocking {
        val testAnimal = Animal(animal_id = "animal123", associated_shelter_id = "shelter123")
        every { mockDocumentReference.delete() } returns Tasks.forResult(null)

        removeAnimalFromDatabase(testAnimal)

        verify(exactly = 1) { mockDocumentReference.delete() }
    }

    @Test
    fun testSetDocumentDataUpdatesDocumentSuccessfully() = runBlocking {
        val testUser = User(user_id = "test123")
        every { mockDocumentReference.set(testUser) } returns Tasks.forResult(null)

        setDocumentData("mocked_collection_name", "test123", testUser)

        verify(exactly = 1) { mockDocumentReference.set(testUser) }
    }

    @Test
    fun testUpdateExplorePreferencesFieldUpdatesFieldSuccessfully() = runBlocking {
        val fieldName: KProperty1<User, *> = User::biography
        val fieldValue = "Updated Preferences"

        every { mockDocumentReference.update(any<String>(), any()) } returns Tasks.forResult(null)

        updateExplorePreferencesField(fieldName, fieldValue)

        verify(exactly = 1) {
            mockDocumentReference.update(
                "explore_preferences.biography",
                fieldValue
            )
        }
    }
}


