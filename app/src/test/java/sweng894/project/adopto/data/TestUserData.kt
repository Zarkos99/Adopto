package sweng894.project.adopto.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.format.DateTimeFormatter

class UserDataTest {

    @Test
    fun testDefaultUserValues() {
        val user = User()

        assertEquals("", user.user_id)
        assertFalse(user.is_shelter)
        assertEquals("", user.biography)
        assertEquals("", user.profile_image_path)
        assertTrue(user.saved_animal_ids.isEmpty())
        assertTrue(user.hosted_animal_ids.isEmpty())
        assertEquals("", user.zip_code)
        assertFalse(user.need_info)
    }

    @Test
    fun testSettingUserValues() {
        val user = User(
            user_id = "123",
            is_shelter = true,
            biography = "Loves animals",
            profile_image_path = "path/to/image.jpg",
            saved_animal_ids = arrayListOf("a1", "a2"),
            hosted_animal_ids = arrayListOf("h1"),
            zip_code = "12345",
            need_info = true
        )

        assertEquals("123", user.user_id)
        assertTrue(user.is_shelter)
        assertEquals("Loves animals", user.biography)
        assertEquals("path/to/image.jpg", user.profile_image_path)
        assertEquals(listOf("a1", "a2"), user.saved_animal_ids)
        assertEquals(listOf("h1"), user.hosted_animal_ids)
        assertEquals("12345", user.zip_code)
        assertTrue(user.need_info)
    }

    @Test
    fun testDefaultAnimalValues() {
        val now = Instant.now()
        val animal = Animal()

        assertEquals("", animal.associated_shelter_id)
        assertEquals("", animal.animal_name)
        assertEquals(0.0, animal.animal_age!!, 0.001)
        assertEquals("", animal.health_summary)
        assertEquals("", animal.biography)
        assertEquals("", animal.profile_image_path)
        assertTrue(animal.supplementary_image_paths.isEmpty())

        val expectedPostTime = DateTimeFormatter.ISO_INSTANT.format(now)
        assertNotNull(animal.post_time)
        assertTrue(animal.post_time!! <= expectedPostTime)
    }

    @Test
    fun testSettingAnimalValues() {
        val animal = Animal(
            associated_shelter_id = "shelter_1",
            animal_name = "Buddy",
            animal_age = 2.5,
            health_summary = "Healthy",
            biography = "Loves to play",
            profile_image_path = "path/to/animal.jpg",
            supplementary_image_paths = arrayListOf("img1.jpg", "img2.jpg"),
            post_time = "2023-01-01T12:00:00Z"
        )

        assertEquals("shelter_1", animal.associated_shelter_id)
        assertEquals("Buddy", animal.animal_name)
        assertEquals(2.5, animal.animal_age!!, 0.001)
        assertEquals("Healthy", animal.health_summary)
        assertEquals("Loves to play", animal.biography)
        assertEquals("path/to/animal.jpg", animal.profile_image_path)
        assertEquals(listOf("img1.jpg", "img2.jpg"), animal.supplementary_image_paths)
        assertEquals("2023-01-01T12:00:00Z", animal.post_time)
    }

    @Test
    fun testUniqueAnimalIdGeneration() {
        val animal1 = Animal()
        val animal2 = Animal()

        assertNotEquals(animal1.animal_id, animal2.animal_id)
    }
}
