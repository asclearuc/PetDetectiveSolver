package md.netinfo.labs.petdetectivesolver.resources

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.io.InputStream


object GameObjects {
    private lateinit var context: Context

    // list of names: ordered by their occurrence on the game levels
    val petNames = listOf("Tabby", "Dachsund", "Hedgehog", "Turtle", "Husky", "Siamese"
                            , "Cockatiel", "Ferret", "Chameleon", "Pug", "Rabbit"
                        )
    val carDirectionNames = listOf("Down", "Left", "Right", "Up")
    val roadNames = listOf("3WayDown", "3WayLeft", "3WayRight", "3WayUp", "CornerBottomLeft",
                        "CornerBottomRight", "CornerTopLeft", "CornerTopRight", "EndDown", "EndLeft",
                        "EndRight", "EndUp", "ThroughHorizontal", "ThroughVertical", "4Way")
    val dotName = "dot1080"

    val roadProps = listOf(
        RoadBlockDescription("3WayDown", false, true, true, true),
        RoadBlockDescription("3WayLeft", true, true, true, false),
        RoadBlockDescription("3WayRight", true, true, false, true),
        RoadBlockDescription("3WayUp", true, false, true, true),
        RoadBlockDescription("CornerBottomLeft", true, false, false, true),
        RoadBlockDescription("CornerBottomRight", true, false, true, false),
        RoadBlockDescription("CornerTopLeft", false, true, false, true),
        RoadBlockDescription("CornerTopRight", false, true, true, false),
        RoadBlockDescription("EndDown", true, false, false, false),
        RoadBlockDescription("EndLeft", false, false, false, true),
        RoadBlockDescription("EndRight", false, false, true, false),
        RoadBlockDescription("EndUp", false, true, false, false),
        RoadBlockDescription("ThroughHorizontal", false, false, true, true),
        RoadBlockDescription("ThroughVertical", true, true, false, false),
        RoadBlockDescription("4Way", true, true, true, true))

    val pets: Map<String, SolverImage> by lazy { loadPets(); }
    val roads: Map<String, SolverImage> by lazy { loadImages(roadNames, "road", "roads"); }
    val cars: Map<String, SolverImage> by lazy { loadImages(carDirectionNames, "car", "cars", SharedGameObjects.prefixCar); }
    val dot: SolverImage by lazy { loadImage("${ASSET_PETS_FOLDER}/${dotName}.png") }

    public fun init(ctx: Context) {
        context = ctx
    }

    private fun loadPets(): Map<String, SolverImage> {
        var result = loadImages(petNames, "pet", "pets", SharedGameObjects.prefixPet)
        loadImages(petNames, "house", "pets", SharedGameObjects.prefixHouse).forEach( {(key, value) -> result[key] = value})
        return result
    }

    private fun loadImages(list: List<String>, prefix: String, desc: String, keyPrefix: String = ""): HashMap<String, SolverImage> {
        var result = HashMap<String, SolverImage>()
        list.forEach {
            try {
                var assetPath = "${ASSET_PETS_FOLDER}/${prefix}${it}.png"
                result["$keyPrefix$it"] = loadImage(assetPath)
            } catch (ioe: IOException) {
                Log.e(TAG, "Failed to load ${desc}", ioe)
                throw ioe
            }
        }
        return result
    }

    fun loadImage(imagePath: String): SolverImage {
        // bitmap part
        // Log.d(TAG, "loadImage(imagePath=$imagePath)")
        val inputStream: InputStream =  context.assets.open(imagePath)
        val bitmap =  BitmapFactory.decodeStream(inputStream)

        return SolverImage(imagePath, bitmap)
    }

    /** Path to pets */
    const val ASSET_PETS_FOLDER: String = "resources"
    /** TAG for logging */
    const val TAG: String = "PDS::GameObjects"
}
