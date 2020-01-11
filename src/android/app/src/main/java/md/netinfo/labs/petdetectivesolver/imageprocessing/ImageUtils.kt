package md.netinfo.labs.petdetectivesolver.imageprocessing

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object ImageUtils {
    fun resizeImage(src: Mat, newWidth: Int): Mat {
        var dst = Mat()
        val sizeRows = src.rows() * newWidth / src.cols()
        Imgproc.resize(src, dst, Size(newWidth.toDouble(), sizeRows.toDouble()))
        return dst
    }

    fun imageToBitmap(image: Mat): Bitmap {
        val img = image.clone()
        var bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(image, bitmap)
        return bitmap
    }

    fun imageToBitmap1(image: Mat?): Bitmap? {
        return if (image != null) imageToBitmap(image) else null
    }

    fun bitmapToImage(bitmap: Bitmap): Mat {
        val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var mat = Mat()
        Utils.bitmapToMat(bmp32, mat)
        return mat
    }

    /**
     * Function to grayscale image
     * @param image Original color image
     * @return Grayscaled image
     */
    fun grayscaleImage(image: Mat?): Mat {
        var grayImage = Mat()
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_RGBA2GRAY)
        return grayImage
    }

    /**
     * Returns true when intersection is found, false otherwise.
     */
    fun isIntersection(r1: FoundObjectInfo, r2: FoundObjectInfo ): Boolean {
        return (getIntersectionArea(r1, r2) > 0)
    }

    /**
     * Returns size of intersected rectangle, 0 otherwise.
     * from: https://stackoverflow.com/questions/14616829/java-method-to-find-the-rectangle-that-is-the-intersection-of-two-rectangles-usi
     */
    fun getIntersectionArea(r1: FoundObjectInfo, r2: FoundObjectInfo ): Int {
        val xmin = Math.max(r1.start.x, r2.start.x)
        val xmax = Math.min(r1.end.x, r2.end.x)
        if (xmax > xmin) {
            val ymin = Math.max(r1.start.y, r2.start.y)
            val ymax = Math.min(r1.end.y, r2.end.y)
            if (ymax > ymin) {
                return (ymax-ymin) * (xmax-xmin)
            }
        }
        return 0
    }

    fun getArea(r: FoundObjectInfo): Int {
        return (r.end.x-r.start.x)*(r.end.y-r.start.y)
    }
}