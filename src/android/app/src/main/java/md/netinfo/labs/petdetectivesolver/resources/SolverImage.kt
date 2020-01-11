package md.netinfo.labs.petdetectivesolver.resources

import android.graphics.Bitmap

data class SolverImage(val assetPath: String
                       , val bitmap: Bitmap
) {
    var name: String = assetPath.split("/").last()
}
