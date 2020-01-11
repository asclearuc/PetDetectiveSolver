package md.netinfo.labs.petdetectivesolver.utils

import android.util.Log

object MemoryInfo {
    // https://stackoverflow.com/questions/2630158/detect-application-heap-size-in-android
    fun dumpMemoryInfo(where: String) {
        val rt = Runtime.getRuntime()
        Log.d(TAG, "Runtime memory info: total=${rt.totalMemory()}, max=${rt.maxMemory()}, free=${rt.freeMemory()} @ $where")
    }

    fun getMemoryInfoTag(): String      = TAG

    /** TAG for logging */
    const val TAG: String = "PDS::MemoryInfo"

}