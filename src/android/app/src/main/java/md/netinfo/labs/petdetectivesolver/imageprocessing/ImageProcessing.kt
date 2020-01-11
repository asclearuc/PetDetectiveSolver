package md.netinfo.labs.petdetectivesolver.imageprocessing

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log

import java.util.*
import kotlin.collections.HashMap

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

import md.netinfo.labs.petdetectivesolver.BuildConfig
import md.netinfo.labs.petdetectivesolver.R
import md.netinfo.labs.petdetectivesolver.exceptions.CarNotFoundException
import md.netinfo.labs.petdetectivesolver.resources.GameObjects
import md.netinfo.labs.petdetectivesolver.model.PDSPoint
import md.netinfo.labs.petdetectivesolver.model.SolveAsyncTask
import md.netinfo.labs.petdetectivesolver.resources.SharedGameObjects
import md.netinfo.labs.petdetectivesolver.solutionfinder.DetailedSolution

/**
 * Class to "parse" Pet Detective game field
 *
 * @property activity Main activity for this class
 * @property image Pet Detective game field to parse
 */
class ImageProcessing(
    val activity: Activity,
    var asyncTask: SolveAsyncTask,
    var processingInfo: ImageProcessingInfo
) {
    private var screenshotScaleFactor: Double = 0.0
    private var mostUsedDimensions: PDSPoint? = null

    private lateinit var foundPets: MutableMap<String, FoundObjectInfo>
    private lateinit var foundDots: MutableList<FoundObjectInfo>

    /**
     * Main function to process Pet Detective game field.
     * It starts objects' recognition process, to find all the objects
     */
    fun recognizeObjects() {
        Log.i(TAG, "About to load image")
        loadImage()
        Log.i(TAG, "About to match pets")
        Log.d(TAG, "Pet size: ${GameObjects.pets.size}")
        Log.d(TAG, "Road size: ${GameObjects.roads.size}")
        Log.d(TAG, "Car size: ${GameObjects.cars.size}")

        // find pets
        foundPets = matchPetsInImage()
        foundPets = processPetInfo(foundPets)

        // find best scale
        val scale = getMostUsedScale(foundPets)

        // add information about car
        val carInfo = matchCarInImage(scale)
        foundPets[carInfo.name] = carInfo

        // detect road under detected pets
        foundPets = matchPetsWithRoadsInImage(foundPets)

        // find all dots
        foundDots = matchDotsInImage()
        resizeDotsToMatchObjects(foundPets, foundDots)

        // clear intersection with car
        adjustCarCoordinates()
        cleanCarIntersections(carInfo.name)

        // clean intersections
        foundDots = cleanDotIntersections(foundPets, foundDots)

        // detect road under detected dots
        foundDots = matchDotsWithRoadInImage(foundDots)

        // resize to original image
        foundPets = convertToImageCoordinates(foundPets)
        foundDots = convertToImageCoordinates(foundDots)

        // clean empty && highly overlapped intersections
        foundDots = cleanEmptyDots(foundDots)
        cleanIntersections(foundPets, foundDots)

        // adjust car coordinates
//        adjustCarCoordinates()

        // draw detected objects
        processingInfo.detectedObjectsImage  = drawDetectedObjectsOnImage(processingInfo.originalImage!!, foundPets, foundDots)
    }

    fun getFoundPets(): Map<String, FoundObjectInfo> = foundPets
    fun getFoundDots(): List<FoundObjectInfo> = foundDots

    fun drawSolution(solutionPath: DetailedSolution) {
        processingInfo.solutionImage = Companion.drawSolution(processingInfo.originalImage!!, solutionPath)
        processingInfo.stage = ImageProcessingInfo.Stage.SOLUTION
    }

    /**
     * Function take current image of the game field, and sequentially applies next operation to it:
     * - resize with width equal to RESIZE_WIDTH
     * - crops central part of it
     * - grayscales it
     * - applies Canny filter to it
     */
    private fun loadImage() {
        // Stage: ORIGINAL
        processingInfo.originalImage = ImageUtils.bitmapToImage(processingInfo.originalBitmap!!)
        Log.d(TAG, "Original image: size (${processingInfo.originalImage?.cols()} x ${processingInfo.originalImage?.rows()})")

        // Stage: resized
        processingInfo.resizedImage = ImageUtils.resizeImage(processingInfo.originalImage!!, RESIZE_WIDTH)
        Log.d(TAG, "Resized image: size (${processingInfo.resizedImage?.cols()} x ${processingInfo.resizedImage?.rows()})")
        processingInfo.stage = ImageProcessingInfo.Stage.RESIZED

        // Stage: cropped
        val r = Rect(
            IMAGE_CROP_LEFT, IMAGE_CROP_TOP,
            processingInfo.resizedImage!!.cols() - IMAGE_CROP_LEFT - IMAGE_CROP_RIGHT,
            processingInfo.resizedImage!!.rows() - IMAGE_CROP_TOP - IMAGE_CROP_BOTTOM
        )
        processingInfo.croppedImage = Mat(processingInfo.resizedImage, r)
        Log.d(TAG, "Cropped image: size (${processingInfo.croppedImage?.cols()} x ${processingInfo.croppedImage?.rows()})")
        processingInfo.stage = ImageProcessingInfo.Stage.CROPPED

        // Stage: grayscaled
        processingInfo.grayscaledImage = ImageUtils.grayscaleImage(processingInfo.croppedImage)
        Log.d(TAG, "Grayscaled imaged: size (${processingInfo.grayscaledImage?.cols()} x ${processingInfo.grayscaledImage?.rows()})")
        processingInfo.stage = ImageProcessingInfo.Stage.GRAYSCALE

        screenshotScaleFactor = processingInfo.resizedImage!!.cols().toDouble() / processingInfo.originalImage!!.cols().toDouble()

        // Stage: apply Canny filter
        // canny for main image
        processingInfo.cannyImage = Mat()
        Imgproc.Canny(
            processingInfo.grayscaledImage,
            processingInfo.cannyImage!!,
            CANNY_MIN_FOR_PETS_DETECTION,
            CANNY_MAX_FOR_PETS_DETECTION
        )
        processingInfo.stage = ImageProcessingInfo.Stage.CANNY
    }

    /**
     * Function looks for each pet on the current game field
     *
     * @return Found objects (as a map: name to FoundObjectInfo)
     */
    private fun matchPetsInImage(): MutableMap<String, FoundObjectInfo> {
        var result = HashMap<String, FoundObjectInfo>()

        for ((key, value) in GameObjects.pets) {
            Log.i(TAG, "looking for object: $key")
            displayMessageOnUiThread("Looking for $key")
            val searchObject = getCannyPetImage(value.bitmap)
            val info: FoundObjectInfo? = matchTemplate(
                key, searchObject, processingInfo.grayscaledImage!!,
                SCALE_MIN_FOR_PETS_DETECTION, SCALE_MAX_FOR_PETS_DETECTION,
                SCALE_STEPS_FOR_PETS_DETECTION
            )

            if (info == null) {
                Log.i(TAG, "Failed to find pet: $key")
                // displayMessageOnUiThread("Failed to find pet: $key", Toast.LENGTH_LONG)
            } else {
                result[key] = info!!
                // displayMessageOnUiThread("Found object '$key' at (${info.start.x}, ${info.start.y}) x (${info.end.x}, ${info.end.y})")
            }
        }

        for ((key, info) in result) {
            Log.i(
                TAG,
                "Found object '$key' at (${info.start.x}, ${info.start.y}) x (${info.end.x}, ${info.end.y}), " +
                        "size (${info.end.x - info.start.x},${info.end.y - info.start.y}), confidence ${info.maxVal}, ratio ${info.ratio}"
            )
        }

        return result
    }

    /**
     * Function looks for a car on the Pet Detective game field.
     * It tests all possible cars (with car direction up/down/bottom/up), and returns the best
     *
     * @param scale Precalculated scale to look for car at
     * @return Information about car
     */
    private fun matchCarInImage(scale: Double): FoundObjectInfo {
        var bestCarInfo: FoundObjectInfo? = null

        for ((key, value) in GameObjects.cars) {
            Log.d(TAG, "Looking for car '$key'")
            val carInfo =
                matchTemplate(key, getCannyPetImage(value.bitmap), processingInfo.grayscaledImage!!, scale, scale, 1)
            if (bestCarInfo == null || bestCarInfo!!.maxVal < carInfo?.maxVal!!) {
                bestCarInfo = carInfo
            }
        }

        if (bestCarInfo == null)
            throw CarNotFoundException(activity.getString(R.string.car_not_found))

        return bestCarInfo!!
    }

    /**
     * Function to detect what kind of road is under each recognized object (e.g. up/down/left/right)
     *
     * @param foundPets Map of recognized objects
     * @return The same map, with updated info regarding road objects
     */
    private fun matchPetsWithRoadsInImage(foundPets: MutableMap<String, FoundObjectInfo>): MutableMap<String, FoundObjectInfo> {
        for ((key, v) in foundPets) {
            val x = v.center.x
            // ROAD_DETECTION_EDGE_THRESHOLD as a hack to correctly determine bottom edge
            v.isDown = checkColourOnImage(
                processingInfo.croppedImage!!, key,
                PDSPoint(x, v.end.y + ROAD_DETECTION_EDGE_THRESHOLD),
                PDSPoint(x, v.center.y),
                PDSPoint(x, v.end.y + ROAD_DETECTION_EDGE_THRESHOLD), 0, -1
            )
            v.isUp = checkColourOnImage(
                processingInfo.croppedImage!!, key,
                PDSPoint(x, v.start.y),
                PDSPoint(x, v.start.y),
                PDSPoint(x, v.center.y), 0, 1
            )
            val y = (v.start.y + (v.end.y - v.start.y) * (when {
                v.name[0] == SharedGameObjects.prefixCar[0] -> {
                    0.55
                }
                else -> {
                    0.75
                }
            })).toInt()
            v.isLeft = checkColourOnImage(
                processingInfo.croppedImage!!, key,
                PDSPoint(v.start.x, y),
                PDSPoint(v.start.x, y),
                PDSPoint(v.center.x, y), 1, 0
            )
            v.isRight = checkColourOnImage(
                processingInfo.croppedImage!!, key,
                PDSPoint(v.end.x, y),
                PDSPoint(v.center.x, y),
                PDSPoint(v.end.x, y), -1, 0
            )
            Log.d(TAG,"pet $key: down=${v.isDown}, up=${v.isUp}, left=${v.isLeft}, right=${v.isRight}")
        }

        return foundPets
    }

    /**
     * Function to match empty intersection in image
     *
     * @return List of found dots
     */
    private fun matchDotsInImage(): MutableList<FoundObjectInfo> {
        Log.d(TAG, "Looking for dots")
        // asclearuc:TODO: move initialization up (to a singleton level?)
        val dotColor: Mat = ImageUtils.bitmapToImage(GameObjects.dot.bitmap)
        // val dotGrayscaled : Mat = ImageUtils.grayscaleImage(dotColor)
        var dotGrayscaled = Mat(dotColor.rows(), dotColor.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(dotColor, dotGrayscaled, Imgproc.COLOR_RGB2GRAY)

        return matchMultiTemplate(
            dotGrayscaled, processingInfo.grayscaledImage!!.clone(),
            SCALE_MIN_FOR_PETS_DETECTION, SCALE_MAX_FOR_PETS_DETECTION,
            SCALE_STEPS_FOR_PETS_DETECTION
        )
    }

    /**
     * Function to detect intersection type under the dot
     *
     * @param List of found dots
     * @return The same list, with type of intersection set
     */
    private fun matchDotsWithRoadInImage(dots: MutableList<FoundObjectInfo>): MutableList<FoundObjectInfo> {
        // there are 2 approaches - bottom-up or top-down
        // use bottom up
        return matchDotsWithRoadInImageBU(dots)
    }

    /**
     *  Function to check consistency of found objects
     *
     *  @param foundPets found objects as map
     *  @return cleaned found objects
     */
    private fun processPetInfo(foundPets: MutableMap<String, FoundObjectInfo>): MutableMap<String, FoundObjectInfo> {
        var result = HashMap<String, FoundObjectInfo>()

        for (pet in GameObjects.petNames) {
            var pInfo: FoundObjectInfo? = null
            var hInfo: FoundObjectInfo? = null
            if (!foundPets.containsKey("${SharedGameObjects.prefixPet}$pet") && foundPets.containsKey("${SharedGameObjects.prefixHouse}$pet")) {
                hInfo = foundPets["${SharedGameObjects.prefixHouse}$pet"]
                Log.i(
                    TAG, "Skipping pet '$pet': it is not present, while its house is found at " +
                            "(${hInfo!!.start.x}, ${hInfo!!.start.y}) x (${hInfo!!.end.x}, ${hInfo!!.end.y})"
                )
                continue
            }
            if (foundPets.containsKey("${SharedGameObjects.prefixPet}$pet") && !foundPets.containsKey("${SharedGameObjects.prefixHouse}$pet")) {
                pInfo = foundPets["${SharedGameObjects.prefixPet}$pet"]
                Log.i(
                    TAG, "Skipping pet '$pet': its house is not found, while it is present at " +
                            "(${pInfo!!.start.x}, ${pInfo!!.start.y}) x (${pInfo!!.end.x}, ${pInfo!!.end.y})"
                )
                continue
            }
            hInfo = foundPets["${SharedGameObjects.prefixHouse}$pet"]
            pInfo = foundPets["${SharedGameObjects.prefixPet}$pet"]
            if (hInfo!!.maxVal < CONFIDENCE_THRESHOLD || pInfo!!.maxVal < CONFIDENCE_THRESHOLD) {
                Log.i(
                    TAG,
                    "Skipping pet $pet: maxVal check failed: pet maxVal: ${pInfo!!.maxVal}, house maxVal: ${hInfo!!.maxVal}"
                )
                continue
            }
            result["${SharedGameObjects.prefixPet}$pet"] = pInfo!!
            result["${SharedGameObjects.prefixHouse}$pet"] = hInfo!!
        }

        var entitiesToRemove: MutableList<String> = mutableListOf<String>()
        for ((key1, value1) in result) {
            for ((key2, value2) in result) {
                // the same key
                if (key1 == key2)
                    continue
                if (entitiesToRemove.contains(key1) || entitiesToRemove.contains(key2))
                    continue
                // consider only houses
                if (key1[0] == SharedGameObjects.prefixHouse[0] || key2[0] == SharedGameObjects.prefixHouse[0])
                    continue
                // check if occupy the same square
                if (Math.abs(value1.start.x - value2.start.x) <= 20 && Math.abs(value1.start.y - value2.start.y) <= 20) {
                    if (value1.maxVal < value2.maxVal) {
                        Log.i(TAG, "Entity intersection: $key1 vs $key2, $key1 has lower value, skipping it")
                        entitiesToRemove.add(key1)
                    } else {
                        Log.i(TAG,"Entity intersection: $key1 vs $key2, $key2 has lower value, skipping it")
                        entitiesToRemove.add(key2)
                    }
                }

            }
        }

        // reprocess all pets
        for (entity in entitiesToRemove) {
            Log.i(TAG, "Entity $entity: removing")
            result.remove(entity)
        }
        // remove houses for pets not in dictionary
        for (petName in GameObjects.petNames) {
            if (result.containsKey("${SharedGameObjects.prefixHouse}$petName") && !result.containsKey("${SharedGameObjects.prefixPet}$petName")) {
                Log.i(TAG, "Pet $petName: removing house")
                result.remove("${SharedGameObjects.prefixHouse}$petName")
            }
        }

        return result
    }

    /**
     * Function to match object (pet or pet house) in the Pet Detective game field
     *
     * @param searchObjectGray object to look for (pet or pet house)
     * @param searchField Pet Detective game field to look for objects in
     * @param scaleMin minimum scale value for search object to look for
     * @param scaleMax maximum scale value for search object to look for
     * @param scaleSteps number of steps to process between @scaleMin and @scaleMax
     */
    private fun matchTemplate(
        name: String, searchObjectGray: Mat, searchField: Mat,
        scaleMin: Double, scaleMax: Double, scaleSteps: Int
    ): FoundObjectInfo? {
        //Log.d(TAG, "matchTemplate: scaleMin=$scaleMin, scaleMax=$scaleMax, scaleSteps=$scaleSteps")
        var bestResult: FoundObjectInfo? = null

        var ratio: Double = 1.0

        val delta = when {
            scaleSteps <= 1 -> {
                maxOf(scaleMax - scaleMin, 1.0)
            }
            else -> {
                (scaleMax - scaleMin) / (scaleSteps - 1)
            }
        }
        var scale = scaleMin
        while (scale <= scaleMax) {
            //Log.d(TAG, "matchTemplate: processing scale $scale, delta is $delta")
            // resize the search field according to the scale, and keep track
            // of the ratio of the resizing
            val searchFieldResized =
                ImageUtils.resizeImage(searchField, (searchField.cols() * scale).toInt())
            ratio = searchField.cols().toDouble() / searchFieldResized.cols().toDouble()

            if ((searchFieldResized.cols() < searchObjectGray.cols()) or (searchFieldResized.rows() < searchObjectGray.rows()))
                break

            // detect edges in the resized, grayscaled search filed and apply search object
            // matching to find the object in the field
            val searchFieldEdged =
                Mat(searchFieldResized.rows(), searchFieldResized.cols(), searchFieldResized.type())
            Imgproc.Canny(
                searchFieldResized,
                searchFieldEdged,
                CANNY_MIN_FOR_PETS_DETECTION,
                CANNY_MAX_FOR_PETS_DETECTION
            )

            //
            val searchResult =
                Mat(searchFieldEdged.rows(), searchFieldEdged.cols(), searchFieldEdged.type())
            Imgproc.matchTemplate(
                searchFieldEdged,
                searchObjectGray,
                searchResult,
                Imgproc.TM_CCOEFF
            )

            val mmr = Core.minMaxLoc(searchResult)
            if ((bestResult == null) || (mmr.maxVal > bestResult!!.maxVal)) {
                bestResult = FoundObjectInfo(
                    name, mmr.maxVal, ratio, scale,
                    PDSPoint(
                        mmr.maxLoc.x * ratio,
                        mmr.maxLoc.y * ratio
                    ),
                    PDSPoint(
                        (mmr.maxLoc.x + searchObjectGray.cols()) * ratio,
                        (mmr.maxLoc.y + searchObjectGray.rows()) * ratio
                    )
                )
            }

            // next iteration
            scale += delta
        }

        return bestResult
    }

    /**
     * Function to match object multiple times. Used for dot detection.
     *
     * @param searchObjectGray object to look for (dot)
     * @param searchField Pet Detective game field to look for objects in
     * @param scaleMin minimum scale value for search object to look for
     * @param scaleMax maximum scale value for search object to look for
     * @param scaleSteps number of steps to process between @scaleMin and @scaleMax
     */
    private fun matchMultiTemplate(
        searchObjectGray: Mat, searchField: Mat,
        scaleMin: Double, scaleMax: Double, scaleSteps: Int
    ): MutableList<FoundObjectInfo> {
        var result = ArrayList<FoundObjectInfo>()

        var edgedImage = Mat(searchObjectGray.rows(), searchObjectGray.cols(), CvType.CV_8UC1)
        // asclearuc:TODO: original Python function call looks like
        // 		edged = cv2.Canny(image, 210, 230, L2gradient = False)
        Imgproc.Canny(
            searchObjectGray,
            edgedImage,
            CANNY_MIN_FOR_DOT_DETECTION,
            CANNY_MAX_FOR_DOT_DETECTION,
            3,
            false
        )

        // match template
        var searchResult = Mat()
        Imgproc.matchTemplate(searchField, edgedImage, searchResult, Imgproc.TM_CCOEFF_NORMED)
        // tempImage = edgedImage

        // threshold to zero - see examples at
        //      https://docs.opencv.org/2.4/doc/tutorials/imgproc/threshold/threshold.html
        //      https://samwize.com/2013/06/09/using-opencv-to-match-template-multiple-times/
        //      https://answers.opencv.org/question/91910/matching-one-template-with-multiple-object-with-opencv-and-java-for-android/
        var mmr = Core.minMaxLoc(searchResult)
        Imgproc.threshold(
            searchResult,
            searchResult,
            THRESHOLD_FOR_DOT_DETECTION,
            mmr.maxVal,
            Imgproc.THRESH_TOZERO
        )

        while (true) {
            mmr = Core.minMaxLoc(searchResult)
            if (mmr.maxVal > THRESHOLD_FOR_DOT_DETECTION) {
                // if(mmr.maxLoc.y<200.0 && mmr.maxLoc.y>65.0)
                // Log.d(TAG, "possible dot: ${mmr.maxLoc} with ${mmr.maxVal} confidence")
                Imgproc.rectangle(
                    searchResult, mmr.maxLoc,
                    Point(mmr.maxLoc.x + edgedImage.cols(), mmr.maxLoc.y + edgedImage.rows()),
                    Scalar(0.0, 255.0, 0.0), Core.FILLED
                )
                val dotInfo = FoundObjectInfo(
                    SharedGameObjects.prefixDot, mmr.maxVal, 1.0, 1.0,
                    PDSPoint(mmr.maxLoc.x, mmr.maxLoc.y),
                    PDSPoint(
                        mmr.maxLoc.x + edgedImage.cols(),
                        mmr.maxLoc.y + edgedImage.rows()
                    )
                )
                if (!checkIfIntersect(result, dotInfo)) {
                    result.add(dotInfo)
                }
            } else {
                break
            }
        }

        Log.d(TAG, "Found ${result.size} potential dots")
        return result
    }

    /**
     * Function to check if the object intersects with any objects from the list
     *
     * @param list List of objects information to be checked against
     * @param info Object to be checked
     * @return True if there is any intersection found, false otherwise
     */
    private fun checkIfIntersect(list: List<FoundObjectInfo>, info: FoundObjectInfo): Boolean {
        var result = false
        for (l in list) {
            if (ImageUtils.isIntersection(l, info)) {
                // Log.d(TAG, "Objects $l and $info intersect")
                result = true
                break
            }
        }
        return result
    }

    /**
     * Function to set dot size equal to the size of the most frequent pet object size.
     *
     * @param foundPets Map of all found pets
     * @param foundDots List of all found dots
     */
    private fun resizeDotsToMatchObjects(
        foundPets: Map<String, FoundObjectInfo>,
        foundDots: MutableList<FoundObjectInfo>
    ) {
        mostUsedDimensions = foundPets.values.map {
            PDSPoint(
                it.end.x - it.start.x,
                it.end.y - it.start.y
            )
        }.groupingBy { it }.eachCount().maxBy { it.value }!!.key
        Log.d(TAG, "Most used dimension is ${mostUsedDimensions!!}")

        // recalculate dot
        for (dot in foundDots) {
            // do not move dot center because it points to the real dot center!!! :)
            // after move, it would be pointing somewhere to the background
            val center = PDSPoint(
                dot.center.x,
                (dot.center.y - 0.29 * (mostUsedDimensions!!.y - (dot.end.y - dot.start.y))).toInt()
            )
            // dot.center = center
            dot.start.y = center.y - mostUsedDimensions!!.y / 2
            dot.end.y = center.y + mostUsedDimensions!!.y / 2
            dot.start.x = center.x - mostUsedDimensions!!.x / 2
            dot.end.x = center.x + mostUsedDimensions!!.x / 2
        }
    }

    /**
     * Function to (e.g. remove) everything that intersects with a car
     */
    private fun cleanCarIntersections(carName: String)
    {
        val car = foundPets[carName]!!

        foundDots = foundDots.filter { ImageUtils.isIntersection(car, it)==false }.toMutableList()
        foundPets = foundPets.filter { (k, v) -> (k==carName ||(k!=carName && !ImageUtils.isIntersection(car, v)))}.toMutableMap()
    }
    /**
     * Clean empty dots (e.g. dots that do not have connections
     */
    private fun cleanEmptyDots(dots: List<FoundObjectInfo>) : MutableList<FoundObjectInfo> {
        Log.d(TAG, "Empty dots: before removing: size ${dots.size}")
        val result = dots.filter { it.isDown || it.isUp || it.isLeft || it.isRight}.toMutableList()
        Log.d(TAG, "Empty dots: after removing: size ${result.size}")
        return result
    }
    /**
     * Function to clean (e.g. remove) dot objects that intersect with pet objects
     *
     * @param pets Map of found pet objects
     * @param dots List of found dot objects
     * @return New list of dot objects, which doesn't include object that intersect with any of the pet objects
     */
    private fun cleanDotIntersections(
        pets: Map<String, FoundObjectInfo>,
        dots: List<FoundObjectInfo>
    ): MutableList<FoundObjectInfo> {
        Log.d(TAG, "Number of found dots before cleaning intersections with pets: ${dots.size}")
        var newDots = ArrayList<FoundObjectInfo>()

        for (d in dots) {
            var doIntersect = false
            for ((k, v) in pets) {
                // skip for car
//                if (k[0] == 'c')
//                    continue
                if (ImageUtils.isIntersection(v, d)) {
                    doIntersect = true
                    break
                }
            }
            if (!doIntersect) {
                newDots.add(d)
            }
        }

        Log.d(TAG, "Number of found dots after cleaning intersections with pets:  ${newDots.size}")
        return newDots
    }

    /**
     * Function to clean intersection of object where intersection size is bigger than the threshold
     * Object with maximum value is kept, and with minimum value is discarded.
     * All operations are in-place
     *
     * @param pets Found pets, all operations are in-place
     * @param dots Found dots, all operations are in-place
     */
    private fun cleanIntersections(
        pets: MutableMap<String, FoundObjectInfo>,
        dots: MutableList<FoundObjectInfo>
    ) {
        var allObjects = pets.values.toMutableList()
        allObjects.addAll(dots)
        pets.clear()
        dots.clear()

        outerloop@ while (true) {
            Log.d(TAG, "List size: ${allObjects.size}")
            for (o1 in allObjects) {
                // Log.d(TAG, "Outer: investigating $o1")
                var doIntersect = false
                for (o2 in allObjects) {
                    // Log.d(TAG, "Inner: investigating $o2")
                    // the same object
                    if (o1 === o2) {
                        continue
                    }
                    val size = ImageUtils.getIntersectionArea(o1, o2)
                    // Log.d(TAG, "Intersection: ${o1.shortName}(${ImageUtils.getArea(o1)}) and ${o2.shortName}(${ImageUtils.getArea(o2)}): size $size")
                    if (size > 0 && size >= ImageUtils.getArea(o1) * THRESHOLD_FOR_INTERSECTION
                        && size >= ImageUtils.getArea(o2) * THRESHOLD_FOR_INTERSECTION
                    ) {
                        val o1t: String = String.format("%.2f", size.toFloat() / ImageUtils.getArea(o1) * 100)
                        val o2t: String = String.format("%.2f", size.toFloat() / ImageUtils.getArea(o2) * 100)
                        Log.d(TAG,"Intersection ${o1.name}(maxVal=${o1.maxVal}) and ${o2.name}(maxVal=${o2.maxVal}) intersect with size $size (intersection: ${o1.name}=${o1t}%, ${o2.name}=${o2t}%, removing object with lower maxVal" )
                        if (o1.maxVal > o2.maxVal) allObjects.remove(o2)
                        else allObjects.remove(o1)
                        continue@outerloop
                    }
                }
            }
            break
        }

        for (o in allObjects) {
            if (o.name[0] == SharedGameObjects.prefixDot[0]) dots.add(o)
            else pets[o.name] = o
        }
    }

    /**
     * Adjust car coordinates. Make them equal to the most used dimensions
     */
    private fun adjustCarCoordinates()
    {
        mostUsedDimensions = foundPets.values.map {
            PDSPoint(
                it.end.x - it.start.x,
                it.end.y - it.start.y
            )
        }.groupingBy { it }.eachCount().maxBy { it.value }!!.key

        var car: FoundObjectInfo = foundPets.values.filter{it.name[0] == SharedGameObjects.prefixCar[0]}[0]
        val center = PDSPoint(
            car.center.x,
            (car.center.y - 4.0 * (mostUsedDimensions!!.y - (car.end.y - car.start.y))).toInt()
        )
        // TODO: repeated piece of code with function resizeDotsToMatchObjects
        Log.d(TAG, "Car center: before: ${car.center}; after $center")
        Log.d(TAG, "Car before: $car")
        car.center = center
        car.start.y = center.y - mostUsedDimensions!!.y / 2
        car.end.y = center.y + mostUsedDimensions!!.y / 2
        car.start.x = center.x - mostUsedDimensions!!.x / 2
        car.end.x = center.x + mostUsedDimensions!!.x / 2

        Log.d(TAG, "Car after : $car")
        foundPets[car.name] = car
    }

    /**
     * Function to get Canny processed object's image (pet or pet house)
     *
     * @param bitmap object's image (pet or pet house)
     * @return Canny processed object's image
     */
    private fun getCannyPetImage(bitmap: Bitmap): Mat {
        var image = ImageUtils.bitmapToImage(bitmap)
        // grayscale it
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2GRAY)
        // canny image
        Imgproc.Canny(image, image, CANNY_MAX_FOR_PETS_DETECTION, CANNY_MAX_FOR_PETS_DETECTION)
        return image
    }

    /**
     * Function to get most used scale
     *
     * @param foundPets Found object (as map)
     * @return Most used scale
     */
    private fun getMostUsedScale(foundPets: MutableMap<String, FoundObjectInfo>): Double {
        val result =
            foundPets.values.map { it.scale }.groupingBy { it }.eachCount().maxBy { it.value }!!.key
        Log.d(TAG, "Most used scale is $result")
        return result
    }

    /**
     * Function to draw detected pets on image
     *
     * @param image Image to draw pets on
     * @param pets Pet pets to be drawn on the image
     * @param dots Dot pets to be drawn on the image
     * @return Image with drawn pets on it
     */
    private fun drawDetectedObjectsOnImage(
        image: Mat,
        pets: Map<String, FoundObjectInfo>,
        dots: List<FoundObjectInfo>
    ): Mat {
        var result = image.clone()
        pets.forEach { drawDetectedObjectOnImage(it.key, it.value, result, COLOR_DETECTED_PET) }
        dots.forEach { drawDetectedObjectOnImage("dot", it, result, COLOR_DETECTED_DOT) }
        processingInfo.stage = ImageProcessingInfo.Stage.DETECTEDOBJECTS
        return result
    }

    /**
     * Function to draw detected object on image
     *
     * @param key Name of detected object
     * @param objectInfo Information about detected object
     * @param result Image to draw object on
     */
    private fun drawDetectedObjectOnImage(key: String, objectInfo: FoundObjectInfo, result: Mat, color: Scalar) {
        Imgproc.rectangle(
            result
            , Point(objectInfo.start.x.toDouble(), objectInfo.start.y.toDouble())
            , Point(objectInfo.end.x.toDouble(), objectInfo.end.y.toDouble())
            , color
            , 2
        )
        // draw centers
        Imgproc.rectangle(
            result,
            Point(objectInfo.center.x - 40.0, objectInfo.center.y - 5.0),
            Point(objectInfo.center.x + 40.0, objectInfo.center.y + 5.0),
            color
        )
        Imgproc.rectangle(
            result,
            Point(objectInfo.center.x - 5.0 , objectInfo.center.y - 40.0),
            Point(objectInfo.center.x + 5.0 , objectInfo.center.y + 40.0),
            color
        )
        val label = FoundObjectInfo.getInfo(key, objectInfo)
        Imgproc.putText(
            result, label
            , Point(objectInfo.start.x.toDouble(), objectInfo.start.y.toDouble())
            , Imgproc.FONT_HERSHEY_COMPLEX, 1.0
            , color
            , 2
        )
    }

    /**
     * Function to detect intersection type under the dot (bottom-up approach)
     *
     * @param List of found dots
     * @return The same list, with type of intersection set
     */
    private fun matchDotsWithRoadInImageBU(dots: MutableList<FoundObjectInfo>): MutableList<FoundObjectInfo> {
        processingInfo.dotsWithRoadsImage = processingInfo.croppedImage!!.clone()

        var idx = 0
        for (d in dots) {
            idx += 1
            val key = "dot$idx"

            var delta = 10

            if (BuildConfig.DEBUG && DEBUG_MATCH_PETS_WITH_ROADS) {
                Log.d(TAG, "dot#${String.format("%02d", idx)}: start=${d.start}, end=${d.end}, center=${d.center}")
            }

            while (
                between(d.start.x - ROAD_DETECTION_EDGE_THRESHOLD, d.end.x + ROAD_DETECTION_EDGE_THRESHOLD, d.center.x, delta)
                && between(d.start.y - ROAD_DETECTION_EDGE_THRESHOLD, d.end.y + ROAD_DETECTION_EDGE_THRESHOLD, d.center.y, delta)
            ) {
                if (BuildConfig.DEBUG && DEBUG_MATCH_PETS_WITH_ROADS) {
                    var arr = processingInfo.dotsWithRoadsImage!!.get(d.center.y - delta, d.center.x - delta)
                    Log.d(TAG, "\t\tstart=(${d.start.x}, ${d.start.y}) end=(${d.end.x}, ${d.end.y}) center=(${d.center.x}, ${d.center.y}): -delta=$delta: (${arr[0].toInt()},${arr[1].toInt()},${arr[2].toInt()})")
                    arr = processingInfo.dotsWithRoadsImage!!.get(d.center.y + delta, d.center.x + delta)
                    Log.d(TAG, "\t\tstart=(${d.start.x}, ${d.start.y}) end=(${d.end.x}, ${d.end.y}) center=(${d.center.x}, ${d.center.y}): +delta=$delta: (${arr[0].toInt()},${arr[1].toInt()},${arr[2].toInt()})")
                }
                if (checkIsBackgroundOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x - delta, d.center.y - delta) &&
                    checkIsBackgroundOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x + delta, d.center.y - delta) &&
                    checkIsBackgroundOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x - delta, d.center.y + delta) &&
                    checkIsBackgroundOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x + delta, d.center.y + delta)
                ) {
                    // found background in both directions
                    break
                }
                delta += 1
            }

            Imgproc.rectangle(
                processingInfo.dotsWithRoadsImage,
                Point(d.center.x - delta.toDouble() - 5, d.center.y - 1.0 - 5),
                Point(d.center.x + delta.toDouble() + 5, d.center.y + 1.0 + 5),
                Scalar(255.0, 255.0, 255.0)
            )
            Imgproc.rectangle(
                processingInfo.dotsWithRoadsImage,
                Point(d.center.x - 1.0 - 5, d.center.y - delta.toDouble() - 5),
                Point(d.center.x + 1.0 + 5, d.center.y + delta.toDouble() + 5),
                Scalar(255.0, 255.0, 255.0)
            )

            d.isDown = checkIsRoadOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x, d.center.y + delta)
            d.isUp = checkIsRoadOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x, d.center.y - delta)
            d.isLeft = checkIsRoadOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x - delta, d.center.y)
            d.isRight = checkIsRoadOnImage(processingInfo.dotsWithRoadsImage!!, d.center.x + delta, d.center.y)
        }

        return dots
    }

    private fun between(l: Int, u: Int, c: Int, d: Int): Boolean =
        ((l <= (c - d)) && ((c + d) <= u))

    /**
     * Function to check colour inside an image between specified points.
     * Color checking is done between start and end point, with initial point set to init
     *
     * @param image Image to check color within
     * @param key Name of the image (used for logging)
     * @param init Initial point to start color checking from
     * @param start Starting point to check color
     * @param end End point to check color
     * @param incX Increment value for X coordinate (used on each step to adjust current position)
     * @param incY Increment value for Y coordinate (used on each step to adjust current position)
     * @return True of false depending if either road was found (true) or background was found (false)
     */
    private fun checkColourOnImage(
        image: Mat, key: String,
        init: PDSPoint, start: PDSPoint, end: PDSPoint,
        incX: Int, incY: Int
    ): Boolean {
        var result: Boolean = false

        // asclearuc:TODO: check if function parameters could be modified inside function
        var x = init.x
        var y = init.y

        while ((start.x <= x) && (x <= end.x) && (start.y <= y) && (y <= end.y)) {
            if (BuildConfig.DEBUG && DEBUG_MATCH_PETS_WITH_ROADS) {
                Log.d(TAG, "color check: $key: dx=$incX, dy=$incY: x=$x y=$y "
                            + "color=(${image[y, x][0]},${image[y, x][1]},${image[y, x][2]})")
                // debugging block
                /*
                if (incX == 0)
                    Imgproc.line(image,  Point((x-3), y), Point((x+3), y), Scalar(255.0,255.0,255.0))
                else
                    Imgproc.line(image,  Point(x, (y-3)), Point(x, (y+3)), Scalar(255.0,255.0,255.0))
                 */
            }
            if (checkIsRoadOnImage(image, x, y)) {
                result = true
                break
            } else if (checkIsBackgroundOnImage(image, x, y)) {
                result = false
                break
            }
            y += incY
            x += incX
        }

        return result
    }

    private fun checkIsRoadOnImage(image: Mat, x: Int, y: Int): Boolean {
        // asclearuc:TODO: may be to move to the constants???
        return checkColorIsBetweenBoundaries(image, x, y, 20, 30)
    }

    private fun checkIsBackgroundOnImage(image: Mat, x: Int, y: Int): Boolean {
        // asclearuc:TODO: may be to move to the constants???
        return checkColorIsBetweenBoundaries(image, x, y, 50, 100)
    }

    private fun checkColorIsBetweenBoundaries(image: Mat, x: Int, y: Int, l: Int, u: Int): Boolean {
        return ((x>=0 && y>=0 && x<image.cols() && y<image.rows()
                && l <= image[y, x][0]) && (l <= image[y, x][1]) && (l <= image[y, x][2])
                && (image[y, x][0] <= u) && (image[y, x][1] <= u) && (image[y, x][2] <= u))
    }

    /**
     * Convert found pets and pet houses to image coordinates
     * @param objects map of found pets and pet houses
     * @return converted map
     */
    private fun convertToImageCoordinates(objects: MutableMap<String, FoundObjectInfo>): MutableMap<String, FoundObjectInfo> {
        for ((k, v) in objects) {
            convertToImageCoordinates(v)
        }
        return objects
    }

    /**
     * Convert found dots to image coordinates
     * @param objects list of found pets and pet houses
     * @return converted list
     */
    private fun convertToImageCoordinates(objects: MutableList<FoundObjectInfo>): MutableList<FoundObjectInfo> {
        for (v in objects) {
            convertToImageCoordinates(v)
        }
        return objects
    }

    /**
     * Convert to image coordinates
     * @param obj object with cooridinates
     */
    private fun convertToImageCoordinates(obj: FoundObjectInfo) {
        obj.start = PDSPoint(
            ((obj.start.x + IMAGE_CROP_LEFT) / screenshotScaleFactor),
            ((obj.start.y + IMAGE_CROP_TOP) / screenshotScaleFactor)
        )
        obj.end = PDSPoint(
            ((obj.end.x + IMAGE_CROP_LEFT) / screenshotScaleFactor),
            ((obj.end.y + IMAGE_CROP_TOP) / screenshotScaleFactor)
        )
        obj.center = PDSPoint(
            ((obj.start.x + obj.end.x) / 2),
            ((obj.start.y + obj.end.y) / 2)
        )
    }

    private fun displayMessageOnUiThread(msg: String) {
        // asclearuc:TODO Toast works too slowly
        // asyncTask.publishProgress(ProgressInfo(null, msg))
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::ImageProcessing"
        /** debug: draw lines on cropped image, may be TODO: move to settings*/
        private val DEBUG_MATCH_PETS_WITH_ROADS = false
        /** Width to resize the image to */
        private val RESIZE_WIDTH = 1080
        /** How much to crop from left */
        private val IMAGE_CROP_LEFT = 0
        /** How much to crop from right */
        private val IMAGE_CROP_RIGHT = 0
        /** How much to crop from top */
        private val IMAGE_CROP_TOP = 200
        /** How much to crop from bottom */
        private val IMAGE_CROP_BOTTOM = 200
        /** min Canny value - TODO: move to settings */
        private val CANNY_MIN_FOR_PETS_DETECTION = 70.0
        /** max Canny value - TODO: move to settings */
        private val CANNY_MAX_FOR_PETS_DETECTION = 90.0
        /** min Canny value - TODO: move to settings */
        private val CANNY_MIN_FOR_DOT_DETECTION = 210.0
        /** max Canny value - TODO: move to settings */
        private val CANNY_MAX_FOR_DOT_DETECTION = 230.0
        /** dot match threshold - TODO: move to settings */
        private val THRESHOLD_FOR_DOT_DETECTION = 0.2
        /** min scale value - TODO: move to settings */
        private val SCALE_MIN_FOR_PETS_DETECTION = 0.72
        /** max scale value - TODO: move to settings */
        private val SCALE_MAX_FOR_PETS_DETECTION = 0.76
        /** number of scale steps - TODO: move to settings */
        private val SCALE_STEPS_FOR_PETS_DETECTION = 5
        /** confidence threshold - TODO: move to settings */
        private val CONFIDENCE_THRESHOLD = 1000000
        /** intersection threshold - if intersection area is bigger than this value, clear drop lowest object*/
        private val THRESHOLD_FOR_INTERSECTION = 0.15
        /** color detected pets should be drawn with */
        private val COLOR_DETECTED_PET = Scalar(0.0, 0.0, 255.0)
        /** color detected dots should be drawn with */
        private val COLOR_DETECTED_DOT = Scalar(255.0, 0.0, 0.0)
        /** value to increase road detection area by */
        private val ROAD_DETECTION_EDGE_THRESHOLD = 10

        fun drawSolution(originalImage: Mat, solutionPath: DetailedSolution) : Mat {
            var result = originalImage.clone()
            for(i in 0 until solutionPath.size) {
                val foundObject: FoundObjectInfo = solutionPath[i]!!.objectInfo!!
                Log.d(TAG, "Processing object $foundObject")
                Imgproc.putText(result
                    , String.format("%02d/%02d", i, solutionPath!![i].carMove.numberOfMoves)
                    , Point(foundObject.start.x.toDouble(), foundObject.start.y.toDouble())
                    , Imgproc.FONT_HERSHEY_COMPLEX, 1.5
                    , Scalar(255.0, 255.0, 255.0)
                    , 2
                )
            }
            return result
        }
    }
}