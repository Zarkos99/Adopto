package sweng894.project.adopto.firebase

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.AdoptoApp
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.AnimalAdoptionInterest
import sweng894.project.adopto.data.AnimalAdoptionInterestedUser
import sweng894.project.adopto.data.AnimalSizes
import sweng894.project.adopto.data.AnimalTypes
import sweng894.project.adopto.data.ExplorationPreferences
import sweng894.project.adopto.data.FirebaseCollections
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
        mockkObject(AdoptoApp)
        every { AdoptoApp.isInitialized() } returns true
        every { AdoptoApp.instance } returns mockk(relaxed = true)

        // Mock Task execution to avoid calling `TaskExecutors.MAIN_THREAD`
        mockkStatic(Tasks::class)
        every { Tasks.forResult(null) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseStorage::class)
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
        getUserData("test123") { result ->

            // Ensure the result is not null and contains the correct data
            assertNotNull(result)
            assertEquals("test123", result?.user_id)
            assertTrue(result?.is_shelter == true)

            // Verify Firestore `.get()` was actually called once
            verify(exactly = 1) { mockDocumentReference.get() }
        }
    }

    @Test
    fun testGetUserDataReturnsNullWhenUserNotFound() = runBlocking {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)
        every { mockSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        getUserData("non_existing_user") { result ->
            assertNull(result)
        }
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

        getAnimalData("animal123", onComplete = { result ->
            assertNotNull("Animal should not be null", result)
            assertEquals("animal123", result!!.animal_id)
            assertEquals("shelter123", result.associated_shelter_id)
        })
    }

    @Test
    fun testGetAnimalDataReturnsNullWhenDocumentDoesNotExist() = runBlocking {
        val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)

        every { mockSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns Tasks.forResult(mockSnapshot)

        getAnimalData("non_existing_animal", onComplete = { result ->
            assertNull("Should return null if document does not exist", result)
        })
    }

    @Test
    fun testGetAnimalDataHandlesFirestoreFailure() = runBlocking {
        every { mockDocumentReference.get() } returns Tasks.forException(RuntimeException("Firestore error"))

        getAnimalData("failing_animal", onComplete = { result ->
            assertNull("Should return null if Firestore query fails", result)
        })
    }

    @Test
    fun testGetAnimalDataHandlesTimeout() = runBlocking {
        getAnimalData("slow_animal", onComplete = { result ->
            assertNull("Should return null if Firestore query times out", result)
        })
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

        addAnimalToDatabaseAndAssociateToShelter(testAnimal)

        verify(exactly = 1) { mockDocumentReference.set(any<Animal>()) }
    }

    @Test
    fun testAddAnimalToDatabaseFailsGracefully() {
        val testAnimal = Animal(animal_id = "A123", associated_shelter_id = "shelter1")
        every { mockDocumentReference.set(any()) } returns Tasks.forException(RuntimeException("Failure"))

        addAnimalToDatabaseAndAssociateToShelter(testAnimal)

        verify { mockDocumentReference.set(testAnimal) }
    }

    @Test
    fun testAddUserToDatabaseLogsOnFailure() {
        every { mockDocumentReference.set(any()) } returns Tasks.forException(RuntimeException("set failed"))

        val testUser = User(user_id = "test_user")
        addUserToDatabase(testUser)

        verify { mockDocumentReference.set(any<User>()) }
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
    fun testFetchAllAnimalsLogsOnFailure() {
        val exception = RuntimeException("fetch fail")
        val mockTask: Task<QuerySnapshot> = mockk(relaxed = true)

        every { mockCollection.get() } returns mockTask

        // Simulate no success callback
        every { mockTask.addOnSuccessListener(any()) } returns mockTask

        // Simulate failure callback being triggered
        every { mockTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTask
        }

        val latch = CountDownLatch(1)

        fetchAllAnimals {
            assertTrue(it.isEmpty()) // The callback should still get triggered
            latch.countDown()
        }

        assertTrue("Callback wasn't triggered", latch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun testRemoveAnimalFromDatabaseSuccessfullyRemovesAnimal() = runBlocking {
        val testAnimal = Animal(animal_id = "animal123", associated_shelter_id = "shelter123")
        every { mockDocumentReference.delete() } returns Tasks.forResult(null)

        removeAnimalFromDatabase(testAnimal)

        verify(exactly = 1) { mockDocumentReference.delete() }
    }

    @Test
    fun testRemoveAnimalFromDatabaseHandlesDeleteFailure() {
        val testAnimal = Animal(animal_id = "animal123", associated_shelter_id = "shelter1")
        every { mockDocumentReference.delete() } returns Tasks.forException(RuntimeException("Delete failed"))

        removeAnimalFromDatabase(testAnimal)

        verify { mockDocumentReference.delete() }
    }

    @Test
    fun testRemoveFromDataFieldArraySuccessfullyRemovesValues() = runBlocking {
        // Arrange
        val mockTask: Task<Void> = Tasks.forResult(null)
        val fieldName: KProperty1<User, *> = User::liked_animal_ids
        val valuesToBeRemoved = arrayOf("A1", "A2")
        val onRemovalSuccessMock: (() -> Unit) = mockk(relaxed = true)

        every { mockDocumentReference.update(any<String>(), any()) } returns mockTask

        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTask
        }

        // Act
        removeFromDataFieldList(
            "mocked_collection_name",
            "test123",
            fieldName,
            valuesToBeRemoved,
            onRemovalSuccessMock
        )

        // Assert
        verify(exactly = 1) {
            mockDocumentReference.update(
                fieldName.name,
                match { it is FieldValue }) // ✅ Fix applied here
        }

        verify(exactly = 1) { onRemovalSuccessMock.invoke() }
    }

    @Test
    fun testRemoveFromDataFieldArrayFails() = runBlocking {
        // Arrange
        val errorMessage = "Firestore update failed"
        val exception = RuntimeException(errorMessage)
        val fieldName: KProperty1<User, *> = User::liked_animal_ids
        val valuesToBeRemoved = arrayOf("A1", "A2")

        val mockTask: Task<Void> = mockk(relaxed = true)

        every { mockDocumentReference.update(any<String>(), any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception) // Manually trigger failure
            mockTask
        }

        // Act
        removeFromDataFieldList("mocked_collection_name", "test123", fieldName, valuesToBeRemoved)

        // Assert
        verify(exactly = 1) {
            mockDocumentReference.update(fieldName.name, match { it is FieldValue })
        }
    }

    @Test
    fun testRemoveFromDataFieldArrayDoesNotSyncIfNotImageField() = runBlocking {
        // Arrange
        mockkStatic(::deleteImagesFromCloudStorage)
        every { deleteImagesFromCloudStorage(any()) } just Runs

        val fieldName: KProperty1<User, *> = User::biography // Does NOT contain "image"
        val valuesToBeRemoved = arrayOf("random_value1", "random_value2")
        val mockTask: Task<Void> = Tasks.forResult(null)

        every { mockDocumentReference.update(any<String>(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTask
        }

        // Act
        removeFromDataFieldList("mocked_collection_name", "test123", fieldName, valuesToBeRemoved)

        // Assert
        verify(exactly = 0) { deleteImagesFromCloudStorage(any()) }
    }


    @Test
    fun testSyncDatabaseForRemovedImagesCallsDeleteImagesFromCloudStorageWhenFieldContainsImage() {
        // Arrange
        mockkStatic(::deleteImagesFromCloudStorage)
        every { deleteImagesFromCloudStorage(any()) } just Runs

        val valuesToBeRemoved = arrayOf("image1.jpg", "image2.png")

        // Act
        syncDatabaseForRemovedImages(valuesToBeRemoved)

        // Assert
        verify(exactly = 1) { deleteImagesFromCloudStorage(valuesToBeRemoved) }
    }

    @Test
    fun testAppendToDataFieldMapDoesNotUpdateInvalidCollection() = runBlocking {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        val fieldName: KProperty1<User, *> = User::explore_preferences
        val fieldKey = "invalid_key"
        val fieldValue = "invalid_value"

        appendToDataFieldMap(
            "invalid_collection",
            "test123",
            fieldName,
            fieldKey,
            fieldValue
        )

        verify(exactly = 0) { mockDocumentReference.update(any<String>(), any()) }

        verify(exactly = 1) {
            Log.e("DEVELOPMENT BUG", "Not a valid collection input.")
        }
    }

    @Test
    fun testAppendToDataFieldMapSuccessfullyUpdatesField() {
        every {
            mockDocumentReference.update(
                "explore_preferences.animal_sizes",
                listOf("small", "medium")
            )
        } returns Tasks.forResult(null)

        appendToDataFieldMap(
            FirebaseCollections.USERS,
            "user1",
            User::explore_preferences,
            ExplorationPreferences::animal_sizes.name,
            listOf("small", "medium")
        )

        verify(exactly = 1) {
            mockDocumentReference.update(
                "explore_preferences.animal_sizes",
                listOf("small", "medium")
            )
        }
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

    @Test
    fun testUpdateUserDisplayNameUpdatesBothAuthAndFirestore() {
        val new_display_name = "New Name"

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        val mock_user = mockk<FirebaseUser>(relaxed = true)
        val mock_firestore = mockk<FirebaseFirestore>(relaxed = true)
        val mock_users_collection = mockk<CollectionReference>(relaxed = true)
        val mock_user_doc = mockk<DocumentReference>(relaxed = true)
        val mock_user_update_task = mockk<Task<Void>>(relaxed = true)

        // Mock Auth
        every { Firebase.auth.currentUser } returns mock_user
        every { mock_user.updateProfile(any()) } returns Tasks.forResult(null)
        every { mock_user.uid } returns "test_user"

        // Mock Firestore
        every { Firebase.firestore } returns mock_firestore
        every { mock_firestore.collection("Users") } returns mock_users_collection
        every { mock_users_collection.document(any()) } returns mock_user_doc
        every { mock_user_doc.set(any<Map<String, Any>>(), any()) } returns mock_user_update_task
        every { mock_user_update_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<Void>>().onSuccess(null)
            mock_user_update_task
        }
        every { mock_user_update_task.addOnFailureListener(any()) } answers {
            mock_user_update_task
        }

        // Act
        updateUserDisplayName(new_display_name)

        // Assert
        verify { mock_user.updateProfile(any()) }

        val capturedMap = slot<Map<String, Any>>()
        verify { mock_user_doc.set(capture(capturedMap), any()) }

        assertEquals(new_display_name, capturedMap.captured["display_name"])
    }


    @Test
    fun testSetDocumentDataSuccess() {
        every { mockDocumentReference.set(any()) } returns Tasks.forResult(null)

        setDocumentData(FirebaseCollections.USERS, "doc1", User(user_id = "doc1"))

        verify { mockDocumentReference.set(any()) }
    }

    @Test
    fun testSetDocumentDataFailsWithInvalidCollection() {
        assertThrows(IllegalArgumentException::class.java) {
            setDocumentData("INVALID_COLLECTION", "doc_id", User(user_id = "123"))
        }
    }

    @Test
    fun testSetDocumentDataLogsErrorOnFailure() {
        every { mockDocumentReference.set(any()) } returns Tasks.forException(RuntimeException("fail"))

        setDocumentData(FirebaseCollections.USERS, "123", User(user_id = "123"))
    }

    @Test
    fun testUpdateDataFieldSuccess() {
        every {
            mockDocumentReference.set(
                match<Map<String, Any?>> { it.containsKey("display_name") && it["display_name"] == "Alice" },
                SetOptions.merge()
            )
        } returns Tasks.forResult(null)

        updateDataField(FirebaseCollections.USERS, "user1", User::display_name, "Alice")

        verify {
            mockDocumentReference.set(
                match<Map<String, Any?>> { it.containsKey("display_name") && it["display_name"] == "Alice" },
                SetOptions.merge()
            )
        }
    }

    @Test
    fun testUpdateDataFieldFailsWithInvalidCollection() {
        assertThrows(IllegalArgumentException::class.java) {
            updateDataField("INVALID", "doc_id", User::display_name, "new_name")
        }
    }

    @Test
    fun testAppendToDataFieldArraySuccess() {
        every {
            mockDocumentReference.set(
                any<Map<String, FieldValue>>(),
                SetOptions.merge()
            )
        } returns Tasks.forResult(null)

        appendToDataFieldArray(
            FirebaseCollections.USERS,
            "user1",
            User::liked_animal_ids,
            "animal123"
        )

        verify { mockDocumentReference.set(any(), SetOptions.merge()) }
    }

    @Test
    fun testAppendToDataFieldArrayFailsWithInvalidCollection() {
        mockkStatic(Log::class)
        every { Log.e("DEVELOPMENT BUG", "Not a valid collection input.") } returns 0

        appendToDataFieldArray("INVALID_COLLECTION", "doc_id", User::liked_animal_ids, "A1")

        verify(exactly = 1) { Log.e("DEVELOPMENT BUG", "Not a valid collection input.") }

        unmockkStatic(Log::class) // Optional: Clean up after test
    }

    @Test
    fun testRecalculatePreferenceVectorHandlesNoUser() {
        every { mockDocumentReference.get() } returns Tasks.forResult(mockk(relaxed = true) {
            every { toObject(User::class.java) } returns null
        })

        recalculatePreferenceVector()
    }

    @Test
    fun testRecalculatePreferenceVectorSkipsWhenNoLikedAnimals() {
        val user = User(user_id = "test_user", liked_animal_ids = mutableListOf())
        every { mockDocumentReference.get() } returns Tasks.forResult(mockk {
            every { toObject(User::class.java) } returns user
        })

        recalculatePreferenceVector()
    }

    @Test
    fun testRecalculatePreferenceVectorSkipsWhenNoValidVectors() {
        val likedAnimalId = "a1"
        val user = User(user_id = "test_user", liked_animal_ids = mutableListOf(likedAnimalId))
        val animalDoc = mockk<DocumentSnapshot> {
            every { toObject(Animal::class.java) } returns Animal(animal_id = likedAnimalId)
        }

        every { mockDocumentReference.get() } returns Tasks.forResult(mockk {
            every { toObject(User::class.java) } returns user
        })

        every {
            mockCollection.whereIn(FieldPath.documentId(), any<List<String>>()).get()
        } returns Tasks.forResult(mockk {
            every { documents } returns listOf(animalDoc)
        })

        recalculatePreferenceVector()
    }

    @Test
    fun testRecalculatePreferenceVector_SkipsUpdateWithNullAnimalFields() {
        val user = User(
            user_id = "u1",
            liked_animal_ids = mutableListOf("a1")
        )
        val userDoc = mockk<DocumentSnapshot> {
            every { toObject(User::class.java) } returns user
        }

        val animalDoc = mockk<DocumentSnapshot> {
            every {
                toObject(Animal::class.java)
            } returns Animal(animal_id = "a1") // No type/size/age — triggers skip
        }

        every { mockDocumentReference.get() } returns Tasks.forResult(userDoc)
        every {
            mockCollection.whereIn(FieldPath.documentId(), any<List<String>>()).get()
        } returns Tasks.forResult(mockk {
            every { documents } returns listOf(animalDoc)
        })

        recalculatePreferenceVector()
    }

    @Test
    fun testGetRecommendationsSkipsIfNoUser() {
        val mock_snap: DocumentSnapshot = mockk(relaxed = true)
        every { mock_snap.toObject(User::class.java) } returns null
        every { mockDocumentReference.get() } returns Tasks.forResult(mock_snap)

        getRecommendations { animals ->
            assertTrue(animals.isEmpty())
        }
    }

    @Test
    fun testGetRecommendationsReturnsFilteredResults() {
        val user = User(
            user_id = "u1",
            liked_animal_ids = mutableListOf(),
            hosted_animal_ids = mutableListOf(),
            adopting_animal_ids = mutableListOf(),
            preference_vector = mapOf("0" to 1.0, "1" to 1.0)
        )
        val animal = Animal(
            animal_id = "a1",
            animal_age = 2.0,
            animal_size = "medium",
            animal_type = "dog",
            location = GeoPoint(0.0, 0.0)
        )

        val userDoc = mockk<DocumentSnapshot> {
            every { toObject(User::class.java) } returns user
        }
        val animalDoc = mockk<DocumentSnapshot> {
            every { toObject(Animal::class.java) } returns animal
        }

        every { mockDocumentReference.get() } returns Tasks.forResult(userDoc)
        every { mockCollection.get() } returns Tasks.forResult(mockk {
            every { documents } returns listOf(animalDoc)
        })

        getRecommendations {
            assertTrue(it.any { a -> a.animal_id == "a1" })
        }
    }


    @Test
    fun fetchInterestedAdoptersForAnimal_returnsExpectedUsers() {
        val animal_id = "animal123"
        val interest = AnimalAdoptionInterest(
            interested_users = mutableListOf(
                AnimalAdoptionInterestedUser("user1"),
                AnimalAdoptionInterestedUser("user2")
            )
        )
        val user1 = User(user_id = "user1")
        val user2 = User(user_id = "user2")

        mockkStatic(FirebaseFirestore::class)
        every { Firebase.firestore } returns mockFirestore

        val adoption_ref = mockk<DocumentReference>()
        val user_collection = mockk<CollectionReference>()
        val user_query = mockk<Query>()

        val doc1 = mockk<QueryDocumentSnapshot>()
        val doc2 = mockk<QueryDocumentSnapshot>()
        every { doc1.toObject(User::class.java) } returns user1
        every { doc2.toObject(User::class.java) } returns user2

        val query_snapshot = mockk<QuerySnapshot>()
        every { query_snapshot.iterator() } returns mutableListOf(doc1, doc2).iterator()

        every {
            mockFirestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref
        every { mockFirestore.collection(FirebaseCollections.USERS) } returns user_collection

        // Adoption get() returns interest
        val adoption_task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val adoption_snapshot = mockk<DocumentSnapshot>()
        every { adoption_snapshot.exists() } returns true
        every { adoption_snapshot.toObject(AnimalAdoptionInterest::class.java) } returns interest
        every { adoption_ref.get() } returns adoption_task
        every { adoption_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(adoption_snapshot)
            adoption_task
        }

        // User query
        every { user_collection.whereIn(eq("user_id"), any<List<String>>()) } returns user_query

        val user_query_task = mockk<Task<QuerySnapshot>>(relaxed = true)
        every { user_query.get() } returns user_query_task
        every { user_query_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<QuerySnapshot>>().onSuccess(query_snapshot)
            user_query_task
        }

        val latch = CountDownLatch(1)
        var result: List<User> = emptyList()

        fetchInterestedAdoptersForAnimal(animal_id) {
            result = it
            latch.countDown()
        }

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))
        assertEquals(2, result.size)
        assertTrue(result.any { it.user_id == "user1" })
        assertTrue(result.any { it.user_id == "user2" })
    }


    @Test
    fun testRemoveUserAdoptionInterest_RemovesCorrectlyWhenUserIsInList() {
        val animal_id = "animal123"
        val user_id = "test_user"

        val mock_adoption_doc = mockk<DocumentSnapshot>(relaxed = true)
        val adoption_interest = AnimalAdoptionInterest(
            interested_users = mutableListOf(AnimalAdoptionInterestedUser(user_id))
        )

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic("sweng894.project.adopto.firebase.FirebaseDatabaseUtilitiesKt")

        every { Firebase.auth.currentUser?.uid } returns user_id
        every { Firebase.firestore } returns mockFirestore

        val adoption_ref = mockk<DocumentReference>(relaxed = true)
        val user_ref = mockk<DocumentReference>(relaxed = true)

        every {
            mockFirestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref

        every {
            mockFirestore.collection(FirebaseCollections.USERS).document(user_id)
        } returns user_ref

        // Manually simulate Firestore get + callback
        every { adoption_ref.get() } answers {
            val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
            every { task.addOnSuccessListener(any()) } answers {
                firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(mock_adoption_doc)
                task
            }
            task
        }

        every {
            mock_adoption_doc.toObject(AnimalAdoptionInterest::class.java)
        } returns adoption_interest

        // Mock the first remove call (from ADOPTIONS collection)
        every {
            removeFromDataFieldList(
                FirebaseCollections.ADOPTIONS,
                animal_id,
                AnimalAdoptionInterest::interested_users,
                any(),
                any()
            )
        } answers {
            lastArg<() -> Unit>().invoke()
        }

        // Mock the second remove call (from USERS collection)
        every {
            removeFromDataFieldList<User>(
                FirebaseCollections.USERS,
                user_id,
                User::adopting_animal_ids,
                animal_id,
                any()
            )
        } answers {
            lastArg<() -> Unit>().invoke()
        }

        val latch = CountDownLatch(1)

        removeUserAdoptionInterest(animal_id) {
            latch.countDown()
        }

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))

        verify {
            removeFromDataFieldList(
                FirebaseCollections.ADOPTIONS,
                animal_id,
                AnimalAdoptionInterest::interested_users,
                any(),
                any()
            )
        }

        verify {
            removeFromDataFieldList(
                FirebaseCollections.USERS,
                user_id,
                User::adopting_animal_ids,
                animal_id,
                any()
            )
        }
    }

    @Test
    fun testRemoveUserAdoptionInterest_RemovesCorrectlyWhenUserNotInList() {
        val animal_id = "animal456"
        val user_id = "test_user"

        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val adoption_ref = mockk<DocumentReference>(relaxed = true)
        val user_doc_ref = mockk<DocumentReference>(relaxed = true)
        val mock_adoption_doc = mockk<DocumentSnapshot>(relaxed = true)
        val adoption_data = AnimalAdoptionInterest(interested_users = mutableListOf())

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)

        every { Firebase.auth.currentUser?.uid } returns user_id
        every { Firebase.firestore } returns firestore

        every {
            firestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref
        every {
            firestore.collection(FirebaseCollections.USERS).document(user_id)
        } returns user_doc_ref

        // Mock the adoption doc get
        val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { adoption_ref.get() } returns task
        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(mock_adoption_doc)
            task
        }
        every { mock_adoption_doc.toObject(AnimalAdoptionInterest::class.java) } returns adoption_data

        // Mock user document update inside removeFromDataFieldList
        val mock_update_task = mockk<Task<Void>>(relaxed = true)
        every { user_doc_ref.update(any<String>(), any()) } returns mock_update_task
        every { mock_update_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<Void>>().onSuccess(null)
            mock_update_task
        }

        val latch = CountDownLatch(1)

        removeUserAdoptionInterest(animal_id) {
            latch.countDown()
        }

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun testRemoveUserAdoptionInterest_HandlesMissingDocument() {
        val animal_id = "animal789"
        val user_id = "test_user"

        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val adoption_ref = mockk<DocumentReference>(relaxed = true)

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)

        every { Firebase.auth.currentUser?.uid } returns user_id
        every { Firebase.firestore } returns firestore
        every {
            firestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref

        val mock_task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { adoption_ref.get() } returns mock_task
        every { mock_task.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            val mock_snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { mock_snapshot.toObject(AnimalAdoptionInterest::class.java) } returns null
            listener.onSuccess(mock_snapshot)
            mock_task
        }

        // NEW - setup User firestore mocks
        val users_collection = mockk<CollectionReference>(relaxed = true)
        val user_document = mockk<DocumentReference>(relaxed = true)
        val user_update_task = mockk<Task<Void>>(relaxed = true)

        every { firestore.collection(FirebaseCollections.USERS) } returns users_collection
        every { users_collection.document(user_id) } returns user_document
        every { user_document.update(any<String>(), any()) } returns user_update_task
        every { user_update_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<Void>>().onSuccess(null)
            user_update_task
        }
        every { user_update_task.addOnFailureListener(any()) } answers {
            user_update_task
        }

        val latch = CountDownLatch(1)

        removeUserAdoptionInterest(animal_id) {
            latch.countDown()
        }

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun testRemoveUserAdoptionInterestHandlesGetFailure() {
        val animal_id = "animal_failure"
        val user_id = "test_user"

        mockkStatic(FirebaseAuth::class)
        every { Firebase.auth.currentUser?.uid } returns user_id
        every {
            mockFirestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id).get()
        } returns Tasks.forException(RuntimeException("Firestore get failed"))

        removeUserAdoptionInterest(animal_id) {
            // Should still be invoked gracefully
        }
    }


    @Test
    fun testRemoveAnimalFromAdoptionInterestCollectionDeletesDocument() {
        every { mockDocumentReference.delete() } returns Tasks.forResult(null)

        removeAnimalFromAdoptionInterestCollection("animal_id")

        verify { mockDocumentReference.delete() }
    }

    @Test
    fun testFetchAnimalsByShelterSuccessAndFailure() {
        val mock_query_snapshot: QuerySnapshot = mockk(relaxed = true)
        val mock_doc: DocumentSnapshot = mockk(relaxed = true)
        every { mock_doc.toObject(Animal::class.java) } returns Animal(animal_id = "A1")
        every { mock_query_snapshot.documents } returns listOf(mock_doc)
        val mock_task: Task<QuerySnapshot> = mockk(relaxed = true)

        val mock_query: Query = mockk(relaxed = true)
        every { mockCollection.whereEqualTo(eq("associated_shelter_id"), any()) } returns mock_query
        every { mock_query.get() } returns mock_task

        every { mock_task.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mock_query_snapshot)
            mock_task
        }

        fetchAnimalsByShelter("shelter_id", onSuccess = {
            assertEquals(1, it.size)
        })

        every { mock_task.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(RuntimeException("Test failure"))
            mock_task
        }

        fetchAnimalsByShelter("shelter_id", onFailure = {
            assertTrue(it is RuntimeException)
        })
    }

    @Test
    fun testFetchAllSheltersLogsFailure() {
        val mock_task: Task<QuerySnapshot> = mockk(relaxed = true)
        val latch = CountDownLatch(1)

        every { mockFirestore.collection(FirebaseCollections.USERS) } returns mockCollection
        every { mockCollection.get() } returns mock_task

        every { mock_task.addOnSuccessListener(any()) } returns mock_task
        every { mock_task.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(RuntimeException("firestore fail"))
            latch.countDown()
            mock_task
        }

        fetchAllShelters { result ->
            println("####TEST_LOG: result = $result")
            assertTrue(result.isEmpty())
        }

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun saveUserAdoptionInterest_addsNewInterestToBothSides() {
        val animal_id = "animal123"
        val user_id = "user456"
        val adoption_interest = AnimalAdoptionInterest(interested_users = mutableListOf())
        val user = User(user_id = user_id)

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic("sweng894.project.adopto.firebase.FirebaseDatabaseUtilitiesKt")

        every { Firebase.auth.currentUser?.uid } returns user_id
        every { Firebase.firestore } returns mockFirestore

        val adoption_ref = mockk<DocumentReference>()
        val user_ref = mockk<DocumentReference>()
        every {
            mockFirestore.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref
        every {
            mockFirestore.collection(FirebaseCollections.USERS).document(user_id)
        } returns user_ref

        // Mock adoption get()
        val adoption_snapshot = mockk<DocumentSnapshot>()
        every { adoption_snapshot.toObject(AnimalAdoptionInterest::class.java) } returns adoption_interest
        val adoption_task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { adoption_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(adoption_snapshot)
            adoption_task
        }
        every { adoption_ref.get() } returns adoption_task

        // Mock user get()
        val user_snapshot = mockk<DocumentSnapshot>()
        every { user_snapshot.toObject(User::class.java) } returns user
        val user_task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { user_task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(user_snapshot)
            user_task
        }
        every { user_ref.get() } returns user_task

        // Capture update success listeners
        val adoption_success_slot = slot<OnSuccessListener<Void>>()
        val user_success_slot = slot<OnSuccessListener<Void>>()

        val adoption_update_task = mockk<Task<Void>>(relaxed = true)
        every { adoption_ref.update(any<String>(), any()) } returns adoption_update_task
        every { adoption_update_task.addOnSuccessListener(capture(adoption_success_slot)) } answers {
            adoption_update_task
        }

        val user_update_task = mockk<Task<Void>>(relaxed = true)
        every { user_ref.update(any<String>(), any()) } returns user_update_task
        every { user_update_task.addOnSuccessListener(capture(user_success_slot)) } answers {
            user_update_task
        }

        val latch = CountDownLatch(1)

        saveUserAdoptionInterest(animal_id, onUploadSuccess = {
            println(">>> Lambda invoked")
            latch.countDown()
        })

        adoption_success_slot.captured.onSuccess(null)
        user_success_slot.captured.onSuccess(null)

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun testSaveUserAdoptionInterest_AlreadyInSync() {
        val adoption_doc = mockk<DocumentSnapshot>(relaxed = true)
        val user_doc = mockk<DocumentSnapshot>(relaxed = true)
        val adoption_ref = mockk<DocumentReference>(relaxed = true)
        val user_ref = mockk<DocumentReference>(relaxed = true)
        val db = mockk<FirebaseFirestore>(relaxed = true)

        val user_id = "test_user"
        val animal_id = "animal123"
        val interest =
            AnimalAdoptionInterest(
                interested_users = mutableListOf(
                    AnimalAdoptionInterestedUser(
                        user_id
                    )
                )
            )
        val user = User(user_id = user_id, adopting_animal_ids = mutableListOf(animal_id))

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns db
        every { Firebase.firestore } returns db
        every { Firebase.auth.currentUser?.uid } returns user_id
        every {
            db.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref
        every {
            db.collection(FirebaseCollections.USERS).document(user_id)
        } returns user_ref
        every { adoption_ref.get() } returns Tasks.forResult(adoption_doc)
        every { adoption_doc.toObject(AnimalAdoptionInterest::class.java) } returns interest
        every { user_ref.get() } returns Tasks.forResult(user_doc)
        every { user_doc.toObject(User::class.java) } returns user

        val latch = CountDownLatch(1)
        saveUserAdoptionInterest(animal_id, onUploadSuccess = { latch.countDown() })
        latch.await(3, TimeUnit.SECONDS)
    }

    @Test
    fun testSaveUserAdoptionInterest_AlreadyInterestedUpdateFails() {
        val adoption_doc = mockk<DocumentSnapshot>(relaxed = true)
        val user_doc = mockk<DocumentSnapshot>(relaxed = true)
        val adoption_ref = mockk<DocumentReference>(relaxed = true)
        val user_ref = mockk<DocumentReference>(relaxed = true)
        val db = mockk<FirebaseFirestore>(relaxed = true)
        val user_id = "test_user"
        val animal_id = "animal456"

        val interest = AnimalAdoptionInterest(
            interested_users = mutableListOf(AnimalAdoptionInterestedUser(user_id))
        )
        val user = User(user_id = user_id, adopting_animal_ids = mutableListOf()) // not recorded

        val mock_failure_task: Task<Void> = mockk(relaxed = true)

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)

        every { Firebase.firestore } returns db
        every { Firebase.auth.currentUser?.uid } returns user_id
        every {
            db.collection(FirebaseCollections.ADOPTIONS).document(animal_id)
        } returns adoption_ref
        every { db.collection(FirebaseCollections.USERS).document(user_id) } returns user_ref

        // Mock adoption_ref.get()
        every { adoption_ref.get() } answers {
            val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
            every { task.addOnSuccessListener(any()) } answers {
                firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(adoption_doc)
                task
            }
            task
        }

        // Mock user_ref.get()
        every { user_ref.get() } answers {
            val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
            every { task.addOnSuccessListener(any()) } answers {
                firstArg<OnSuccessListener<DocumentSnapshot>>().onSuccess(user_doc)
                task
            }
            task
        }

        every { adoption_doc.toObject(AnimalAdoptionInterest::class.java) } answers {
            println("Returning adoption interest: $interest")
            interest
        }

        every { user_doc.toObject(User::class.java) } answers {
            println("Returning user object: $user")
            user
        }

        // Simulate update failure
        every { user_ref.update(User::adopting_animal_ids.name, any()) } returns mock_failure_task
        every { mock_failure_task.addOnSuccessListener(any()) } returns mock_failure_task

        val latch = CountDownLatch(1)

        every { mock_failure_task.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(RuntimeException("Simulated failure"))
            latch.countDown()
            mock_failure_task
        }

        saveUserAdoptionInterest(
            animal_id = animal_id,
            onUploadSuccess = { fail("Should not call success callback") },
            onUploadFailure = { /* expected path */ }
        )

        assertTrue("Callback not triggered", latch.await(3, TimeUnit.SECONDS))

        verify(exactly = 1) {
            user_ref.update(User::adopting_animal_ids.name, any())
        }
    }

    @Test
    fun testUpdateExplorePreferencesField_WithArrayConvertsToList() {
        val test_array = arrayOf("small", "medium")
        val test_path = "explore_preferences.animal_sizes"

        every {
            mockDocumentReference.update(
                test_path,
                test_array.toList()
            )
        } returns Tasks.forResult(null)

        updateExplorePreferencesField(ExplorationPreferences::animal_sizes, test_array)

        verify { mockDocumentReference.update(test_path, test_array.toList()) }
    }

    @Test
    fun testRemoveFromDataFieldListFailsGracefully() {
        val exception = RuntimeException("Test failure")

        every {
            mockDocumentReference.update(
                match<String> { it == User::liked_animal_ids.name },
                any()
            )
        } returns Tasks.forException(exception)

        removeFromDataFieldList("mocked_collection_name", "doc123", User::liked_animal_ids, "A1")
    }

    @Test
    fun testAnimalWithinSearchParametersPassesAllFilters() {
        val user = User(
            explore_preferences = ExplorationPreferences(
                search_radius_miles = 500.0,
                animal_sizes = mutableListOf(AnimalSizes.MEDIUM), // use constant
                animal_types = mutableListOf(AnimalTypes.DOG),    // use constant
                min_animal_age = 1.0,
                max_animal_age = 10.0
            ),
            location = GeoPoint(38.8951, -77.0364)
        )

        val animal = Animal(
            animal_id = "a1",
            location = GeoPoint(39.0, -77.0),
            animal_size = "medium", // this will normalize correctly
            animal_type = "dog",    // this too
            animal_age = 5.0
        )

        assertTrue(
            "Animal should pass all filters",
            animalWithinSearchParameters(user, animal, user.location)
        )
    }


    @Test
    fun testAnimalWithinSearchParametersRejectsByDistance() {
        val user = User(
            explore_preferences = ExplorationPreferences(search_radius_miles = 10.0),
            location = GeoPoint(38.8951, -77.0364) // Washington DC
        )
        val animal = Animal(
            animal_id = "a1",
            location = GeoPoint(40.7128, -74.0060) // New York City (farther than 10 miles)
        )

        val result = animalWithinSearchParameters(user, animal, user.location)
        assertFalse("Animal should be filtered out due to distance", result)
    }

    @Test
    fun testAnimalWithinSearchParametersRejectsBySize() {
        val user =
            User(explore_preferences = ExplorationPreferences(animal_sizes = mutableListOf("small")))
        val animal = Animal(animal_id = "a1", animal_size = "large")

        val result = animalWithinSearchParameters(user, animal, null)
        assertFalse(result)
    }

    @Test
    fun testAnimalWithinSearchParametersRejectsByType() {
        val user =
            User(explore_preferences = ExplorationPreferences(animal_types = mutableListOf("cat")))
        val animal = Animal(animal_id = "a1", animal_type = "dog")

        assertFalse(animalWithinSearchParameters(user, animal, null))
    }

    @Test
    fun testAnimalWithinSearchParametersRejectsByAgeRange() {
        val user = User(
            explore_preferences = ExplorationPreferences(
                min_animal_age = 2.0,
                max_animal_age = 5.0
            )
        )
        val animal = Animal(animal_id = "a1", animal_age = 10.0)

        assertFalse(animalWithinSearchParameters(user, animal, null))
    }

    @Test
    fun testAnimalWithinSearchParameters_NoLocation_ReturnsTrue() {
        val user = User(
            explore_preferences = ExplorationPreferences(
                animal_sizes = mutableListOf(AnimalSizes.SMALL),
                animal_types = mutableListOf(AnimalTypes.DOG),
                min_animal_age = 1.0,
                max_animal_age = 10.0
            )
        )
        val animal = Animal(
            animal_id = "doggo1",
            animal_size = "small", // input
            animal_type = "dog",   // input
            animal_age = 3.0       // in between 1 and 10
        )

        val result = animalWithinSearchParameters(user, animal, null)
        assertTrue("Animal should pass when no location is provided", result)
    }


    @Test
    fun testHaversineDistanceCorrectness() {
        val dc = GeoPoint(38.8951, -77.0364)
        val ny = GeoPoint(40.7128, -74.0060)

        val distance = haversineDistance(dc, ny)

        assertTrue("Expected roughly 200-250 miles", distance in 200.0..250.0)
    }


}


