package md.netinfo.labs.petdetectivesolver.utils

import android.util.Log

import md.netinfo.labs.petdetectivesolver.BuildConfig
import md.netinfo.labs.petdetectivesolver.solutionfinder.DetailedSolution

object SolutionUtils {
    /**
     * Write solution to log file
     * @param solutionMovesCount Number of moves
     * @param solutionPath Solution
     */
    fun writeSolutionToLog(solutionMovesCount: Int, solutionPath: DetailedSolution?)
    {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Number of moves: $solutionMovesCount")
            if(solutionPath==null) {
                Log.e(TAG, "Solution path is NULL")
            } else {
                for(i in 0 until solutionPath!!.size) {
                    Log.d(TAG, "Step $i: ${solutionPath!![i]}")
                }
            }
        }
    }

    const val TAG: String = "PDS::SolutionUtils"

}