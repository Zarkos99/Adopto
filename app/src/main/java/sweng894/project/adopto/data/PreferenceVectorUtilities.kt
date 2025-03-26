package sweng894.project.adopto.data

import android.content.Context
import android.util.Log
import sweng894.project.adopto.R

object VectorUtils {

    private const val TAG = "VectorUtils"

    private lateinit var TYPE_ENCODING: Map<String, Double>
    private val SIZE_ENCODING = mapOf("Small" to 1.0, "Medium" to 2.0, "Large" to 3.0)

    fun initializeTypeEncoding(context: Context) {
        val animal_types = context.resources.getStringArray(R.array.animal_types)
        TYPE_ENCODING = animal_types.mapIndexed { index, type ->
            type to (index + 1).toDouble()
        }.toMap()

        Log.d(TAG, "Initialized TYPE_ENCODING: $TYPE_ENCODING")
    }

    fun animalToVector(animal: Animal): List<Double> {
        val typeValue = TYPE_ENCODING[animal.animal_type] ?: 0.0
        val ageValue = animal.animal_age ?: 0.0
        val sizeValue = SIZE_ENCODING[animal.animal_size] ?: 0.0

        val vector = listOf(typeValue, ageValue, sizeValue)

        Log.d(TAG, "Converted animal (${animal.animal_id}) to vector: $vector")
        return vector
    }

    fun averageVectors(vectors: List<List<Double>>): List<Double> {
        if (vectors.isEmpty()) {
            Log.d(TAG, "averageVectors: Input list is empty. Returning empty vector.")
            return listOf()
        }

        val dimension = vectors[0].size
        val sumVector = MutableList(dimension) { 0.0 }

        for (v in vectors) {
            for (i in 0 until dimension) {
                sumVector[i] += v[i]
            }
        }

        val avgVector = sumVector.map { it / vectors.size }

        Log.d(TAG, "Calculated average vector: $avgVector")
        return avgVector
    }

    fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        val dot = a.zip(b).sumOf { it.first * it.second }
        val magA = kotlin.math.sqrt(a.sumOf { it * it })
        val magB = kotlin.math.sqrt(b.sumOf { it * it })

        val similarity = if (magA == 0.0 || magB == 0.0) 0.0 else dot / (magA * magB)

        Log.d(TAG, "Cosine similarity between $a and $b = $similarity")
        return similarity
    }
}
