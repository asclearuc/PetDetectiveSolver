package md.netinfo.labs.petdetectivesolver.imageprocessing

import android.graphics.Bitmap
import md.netinfo.labs.petdetectivesolver.gameboard.GameBoard
import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import org.opencv.core.Mat

class ImageProcessingInfo(var originalBitmap: Bitmap?) {
    enum class Stage {
        ORIGINAL,
        RESIZED,
        CROPPED,
        GRAYSCALE,
        CANNY,
        DOTSWITHROADS,
        DETECTEDOBJECTS,
        SOLUTION
    }
    var stage: Stage = Stage.ORIGINAL
    var originalImage: Mat? = null
    var resizedImage: Mat? = null
    var croppedImage: Mat? = null
    var grayscaledImage: Mat? = null
    var cannyImage: Mat? = null
    var dotsWithRoadsImage: Mat? = null
    var detectedObjectsImage: Mat? = null
    var solutionImage: Mat? = null

    var resizedBitmap: Bitmap? = null
    var croppedBitmap: Bitmap? = null
    var grayscaledBitmap: Bitmap? = null
    var cannyBitmap: Bitmap? = null
    var dotsWithRoadsBitmap: Bitmap? = null
    var detectedObjectsBitmap: Bitmap? = null
    var solutionBitmap: Bitmap? = null

    var gameBoard: GameBoard? = null
    var simpleGameBoard: SimpleGameBoardData? = null

    var throwable: Throwable? = null
    var puzzleId: String = ""

    fun getLastStageBitmap(): Bitmap? {
        return when (stage) {
            Stage.ORIGINAL              -> { return getOriginalImage() }
            Stage.RESIZED               -> { return getResizedImage() }
            Stage.CROPPED               -> { return getCroppedImage() }
            Stage.GRAYSCALE             -> { return getGrayscaledImage() }
            Stage.CANNY                 -> { return getCannyImage() }
            Stage.DOTSWITHROADS         -> { return getDotsWithRoadsImage() }
            Stage.DETECTEDOBJECTS       -> { return getDetectedObjectsImage() }
            Stage.SOLUTION              -> { return getSolutionImage() }
        }
    }

    fun getOriginalImage(): Bitmap? {
        if (originalBitmap == null)   originalBitmap = ImageUtils.imageToBitmap1(originalImage)
        return originalBitmap
    }
    fun getResizedImage(): Bitmap? {
        if (resizedBitmap == null)   resizedBitmap = ImageUtils.imageToBitmap1(resizedImage)
        return resizedBitmap
    }
    fun getCroppedImage(): Bitmap? {
        if (croppedBitmap == null)   croppedBitmap = ImageUtils.imageToBitmap1(croppedImage)
        return croppedBitmap
    }
    fun getGrayscaledImage(): Bitmap? {
        if (grayscaledBitmap == null)   grayscaledBitmap = ImageUtils.imageToBitmap1(grayscaledImage)
        return grayscaledBitmap
    }
    fun getCannyImage(): Bitmap? {
        if (cannyBitmap == null)   cannyBitmap = ImageUtils.imageToBitmap1(cannyImage)
        return cannyBitmap
    }
    fun getDotsWithRoadsImage(): Bitmap? {
        if (dotsWithRoadsBitmap==null)  dotsWithRoadsBitmap = ImageUtils.imageToBitmap1(dotsWithRoadsImage)
        return dotsWithRoadsBitmap
    }
    fun getDetectedObjectsImage(): Bitmap? {
        if (detectedObjectsBitmap == null)   detectedObjectsBitmap = ImageUtils.imageToBitmap1(detectedObjectsImage)
        return detectedObjectsBitmap
    }
    fun getSolutionImage(): Bitmap? {
        if (solutionBitmap == null)   solutionBitmap= ImageUtils.imageToBitmap1(solutionImage)
        return solutionBitmap
    }

}