package md.netinfo.labs.petdetectivesolver.model

import android.content.Context
import android.util.Log
import java.io.IOException
import kotlin.random.Random

import md.netinfo.labs.petdetectivesolver.resources.GameObjects
import md.netinfo.labs.petdetectivesolver.resources.SolverImage

class SampleImageStorage(val context: Context) {
    enum class RandomBehaviour {
        RANDOM,
        SEQUENTIAL,
        PREDEFINED
    }

    var currentImageIndex = 0

    val images: List<SolverImage> by lazy { loadImages() }

    public fun getRandomImage(): SolverImage {
        return when (RANDOM_BEHAVIOUR) {
            RandomBehaviour.RANDOM          -> { images.get(Random.nextInt(images.size)) }
            RandomBehaviour.SEQUENTIAL      -> {
                val returnValue = images.get(currentImageIndex)
                currentImageIndex = (currentImageIndex + 1) % images.size
                returnValue
            }
            RandomBehaviour.PREDEFINED      -> {
                images.filter { it.assetPath.contains(RANDOM_PREDEFINED_VALUE) }[0]
            }
        }
    }

    private fun loadImages(): List<SolverImage> {
        var names: Array<String>?
        var result: ArrayList<SolverImage> = ArrayList<SolverImage>()
        try {
            names = context.assets.list(SAMPLE_IMAGES_FOLDER)
        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to load assets")
            return result
        }

        names?.forEach {
            try {
                val assetPath = "$SAMPLE_IMAGES_FOLDER/$it"
                result.add(GameObjects.loadImage(assetPath))
            } catch (ioe: IOException) {
                Log.e(TAG, "Failed to load assets")
            }
        }

        return result
    }

    companion object {
        /**
         * Folder with images
         */
        private val SAMPLE_IMAGES_FOLDER = "sample_images"
        /**
         * TAG for logging
         */
        private val TAG = "PDS::SampleImageStorage"
        /**
         * Behaviour for random values
         * asclearuc:TODO: move to settings
         */
        private val RANDOM_BEHAVIOUR = RandomBehaviour.SEQUENTIAL
        /**
         * Predefined value - always return it instead of random value
         * asclearuc:TODO: additional checks needed (e.g. behaviour of such value doesn't exist)
         * asclearuc:TODO: move to settings?
         */
        private val RANDOM_PREDEFINED_VALUE = "Screenshot_20190203-161914.jpg"
    }
}