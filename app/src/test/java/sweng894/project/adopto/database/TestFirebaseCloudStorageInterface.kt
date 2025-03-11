package sweng894.project.adopto.database

import com.google.android.gms.tasks.OnSuccessListener
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.*
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.User

class FirebaseStorageUtilsTest {

    private lateinit var mockStorage: FirebaseStorage
    private lateinit var mockStorageRef: StorageReference
    private lateinit var mockUploadTask: UploadTask
    private lateinit var mockFirebaseDataServiceUsers: FirebaseDataServiceUsers
    private lateinit var mockUser: User
    private lateinit var mockUri: Uri
    private val mockContext = mockk<Context>(relaxed = true)
    private lateinit var mockImageView: ImageView

    @Before
    fun setUp() {
        // Mock FirebaseApp initialization
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp
        every { FirebaseApp.getInstance() } returns mockFirebaseApp

        // ✅ Mock FirebaseAuth.getInstance() properly
        mockkStatic(FirebaseAuth::class)
        val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockFirebaseAuth

        // Mock Firebase Storage
        mockkStatic(FirebaseStorage::class)
        mockStorage = mockk(relaxed = true)
        mockStorageRef = mockk(relaxed = true)
        mockUploadTask = mockk(relaxed = true)

        every { FirebaseStorage.getInstance() } returns mockStorage
        every { mockStorage.reference } returns mockStorageRef

        // Mock FirebaseDataServiceUsers
        mockFirebaseDataServiceUsers = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        every { mockFirebaseDataServiceUsers.current_user_data } returns mockUser
        mockkStatic(::getCurrentUserId)
        every { getCurrentUserId() } returns "mockUserId"


        // Mock Uri
        mockUri = mockk(relaxed = true)
        every { mockUri.path } returns "mock/path/to/image.jpg"

        // Mock Strings to prevent resource dependency errors
        mockkObject(Strings)
        every { Strings.get(any()) } returns "mocked_firebase_collection"

        // Mock Glide usage (for loading images)
        mockImageView = mockk(relaxed = true)
    }

    @Test
    fun uploadUserProfileImageAndUpdateUserImagePath() {
        val successSlot = slot<OnSuccessListener<UploadTask.TaskSnapshot>>()

        // Setup user profile image deletion if an existing path exists
        val oldImagePath = "mocked_firebase_collection/mockUserId/profile_image"
        every { mockUser.profile_image_path } returns oldImagePath

        val mockOldImageRef = mockk<StorageReference>(relaxed = true)
        every { mockStorageRef.child(oldImagePath) } returns mockOldImageRef
        every { mockOldImageRef.delete() } returns Tasks.forResult(null)

        mockkStatic(::deleteImagesFromCloudStorage)

        // Mock upload behavior
        every { mockStorageRef.child(any()) } returns mockStorageRef
        every { mockStorageRef.putFile(mockUri) } returns mockUploadTask
        every { mockUploadTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockk(relaxed = true)) // Manually trigger the captured listener
            mockUploadTask
        }

        // Call function
        uploadUserProfileImageAndUpdateUserImagePath(mockFirebaseDataServiceUsers, mockUri)

        // Verify deletion and upload processes
        verify(exactly = 2) { mockStorageRef.child(oldImagePath) } // Ensure it tried to access the path
        verify(exactly = 1) { deleteImagesFromCloudStorage(arrayOf(oldImagePath)) }
        verify(exactly = 1) { mockStorageRef.putFile(mockUri) } // Ensure upload happened
    }

    @Test
    fun uploadAnimalImageAndUpdateAnimalProfileImage() {
        val animalId = "testAnimal123"
        val successListenerSlot = slot<OnSuccessListener<UploadTask.TaskSnapshot>>()

        // Mock upload behavior
        every { mockStorageRef.child(any()) } returns mockStorageRef
        every { mockStorageRef.putFile(mockUri) } returns mockUploadTask
        every { mockUploadTask.addOnSuccessListener(capture(successListenerSlot)) } answers {
            successListenerSlot.captured.onSuccess(mockk(relaxed = true))
            mockUploadTask
        }


        // Call function
        uploadAnimalImageAndUpdateAnimal(animalId, mockUri, is_profile_image = true)

        // Verify upload to correct path
        verify(exactly = 1) { mockStorageRef.child(match { it.contains("profile_image") }) }
        verify(exactly = 1) { mockStorageRef.putFile(mockUri) }
    }

    @Test
    fun uploadAnimalImageAndUpdateAnimalSupplementaryImage() {
        val animalId = "testAnimal123"
        val successListenerSlot =
            slot<OnSuccessListener<UploadTask.TaskSnapshot>>() // ✅ Capture OnSuccessListener

        // Mock upload behavior
        every { mockStorageRef.child(any()) } returns mockStorageRef
        every { mockStorageRef.putFile(mockUri) } returns mockUploadTask
        every { mockUploadTask.addOnSuccessListener(capture(successListenerSlot)) } answers {
            successListenerSlot.captured.onSuccess(mockk(relaxed = true)) // ✅ Trigger success manually
            mockUploadTask
        }

        // Call function
        uploadAnimalImageAndUpdateAnimal(animalId, mockUri, is_profile_image = false)

        // Verify upload to correct path
        verify(exactly = 1) { mockStorageRef.child(match { it.contains("supplementary_images") }) }
        verify(exactly = 1) { mockStorageRef.putFile(mockUri) }
    }

    @Test
    fun deleteImagesFromCloudStorageDeletesImages() {
        val imagePaths = arrayOf("path1", "path2")

        // Create mock StorageReferences for each path
        val mockImageRef1 = mockk<StorageReference>(relaxed = true)
        val mockImageRef2 = mockk<StorageReference>(relaxed = true)

        // Mock `child()` to return specific references
        every { mockStorageRef.child("path1") } returns mockImageRef1
        every { mockStorageRef.child("path2") } returns mockImageRef2

        // Mock `delete()` calls
        every { mockImageRef1.delete() } returns Tasks.forResult(null)
        every { mockImageRef2.delete() } returns Tasks.forResult(null)

        // Call function
        deleteImagesFromCloudStorage(imagePaths)

        // Verify that each specific reference's `delete()` was called
        verify(exactly = 1) { mockImageRef1.delete() }
        verify(exactly = 1) { mockImageRef2.delete() }
    }

    @Test
    fun loadCloudStoredImageIntoImageViewLoadsImage() {
        val imagePath = "mock/path/to/image.jpg"
        val mockImageRef = mockk<StorageReference>(relaxed = true)
        val mockUriResult = Tasks.forResult(Uri.parse("https://fakeurl.com/image.jpg"))

        // Mock download URL retrieval
        every { mockStorageRef.child(imagePath) } returns mockImageRef
        every { mockStorageRef.downloadUrl } returns mockUriResult

        // Call function
        loadCloudStoredImageIntoImageView(mockContext, imagePath, mockImageView)

        // Verify download URL request
        verify(exactly = 1) { mockImageRef.downloadUrl }
    }

}
