package sweng894.project.adopto.data

import android.content.Context
import android.content.res.Resources
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sweng894.project.adopto.R

class TestPreferenceVectorUtilities {

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    @Before
    fun setup() {
        mockContext = mockk()
        mockResources = mockk()

        every { mockContext.resources } returns mockResources
        every { mockResources.getStringArray(R.array.animal_types) } returns arrayOf(
            "Dog",
            "Cat",
            "Bird"
        )

        VectorUtils.initializeTypeEncoding(mockContext)
    }

    @Test
    fun testAnimalToVector_correctMapping() {
        val animal = Animal(
            animal_id = "a1",
            animal_type = "Dog",
            animal_age = 3.0,
            animal_size = "Medium"
        )

        val vector = VectorUtils.animalToVector(animal)

        // Dog → 1.0 (index 0 + 1), Medium → 2.0, age is directly used
        assertEquals(listOf(1.0, 3.0, 2.0), vector)
    }

    @Test
    fun testAnimalToVector_withUnknownTypeAndSize_defaultsToZero() {
        val animal = Animal(
            animal_id = "a2",
            animal_type = "Lizard", // not in initialized list
            animal_age = 5.0,
            animal_size = "Giant"   // not in SIZE_ENCODING
        )

        val vector = VectorUtils.animalToVector(animal)

        assertEquals(listOf(0.0, 5.0, 0.0), vector)
    }

    @Test
    fun testAverageVectors_computesMeanCorrectly() {
        val vectors = listOf(
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0, 6.0),
            listOf(7.0, 8.0, 9.0)
        )

        val avg = VectorUtils.averageVectors(vectors)

        assertEquals(listOf(4.0, 5.0, 6.0), avg)
    }

    @Test
    fun testAverageVectors_emptyList_returnsEmpty() {
        val avg = VectorUtils.averageVectors(emptyList())

        assertTrue(avg.isEmpty())
    }

    @Test
    fun testCosineSimilarity_identicalVectors_returnsOne() {
        val a = listOf(1.0, 2.0, 3.0)
        val b = listOf(1.0, 2.0, 3.0)

        val sim = VectorUtils.cosineSimilarity(a, b)

        assertEquals(1.0, sim, 0.0001)
    }

    @Test
    fun testCosineSimilarity_orthogonalVectors_returnsZero() {
        val a = listOf(1.0, 0.0)
        val b = listOf(0.0, 1.0)

        val sim = VectorUtils.cosineSimilarity(a, b)

        assertEquals(0.0, sim, 0.0001)
    }

    @Test
    fun testCosineSimilarity_zeroMagnitude_returnsZero() {
        val a = listOf(0.0, 0.0)
        val b = listOf(1.0, 2.0)

        val sim = VectorUtils.cosineSimilarity(a, b)

        assertEquals(0.0, sim, 0.0001)
    }
}
