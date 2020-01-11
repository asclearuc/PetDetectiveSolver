package md.netinfo.labs.petdetectivesolver.model

import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import md.netinfo.labs.petdetectivesolver.R
import md.netinfo.labs.petdetectivesolver.exceptions.NoSolutionFoundException
import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessing

import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessingInfo
import md.netinfo.labs.petdetectivesolver.solutionfinder.GameboardSolutionFinder
import md.netinfo.labs.petdetectivesolver.solutionfinder.Solution
import md.netinfo.labs.petdetectivesolver.utils.SolutionUtils
import md.netinfo.labs.petdetectivesolver.view.MainActivity

class DownloadSolutionAsyncTask(var activity: MainActivity) : AsyncTask<Void, Void, Void>() {
    var solvingInfo: ImageProcessingInfo = activity.imageProcessingResult!!
    var solution: Solution? = null
    var exception: Throwable? = null

    override fun doInBackground(vararg params: Void): Void? {
        Log.d(TAG, "doInBackground - start")
        var sd = SolutionDownloader()

        try {
            solution = sd.downloadSolution(solvingInfo.puzzleId)
        } catch (e: Throwable) {
            exception = e
            Log.e(TAG, "Exception occured while retrieving solution for id '${solvingInfo.puzzleId}", e )
        }

        Log.d(TAG, "doInBackground - before return")
        return null
    }

    override fun onPostExecute(result: Void?) {
        when {
            solution != null                -> { processSolution() }
            exception != null               -> {
                solvingInfo!!.throwable = exception
                activity.showProcessingErrorMessage() }
            else                            -> {
                Toast.makeText(activity.applicationContext, activity.getString(R.string.no_solution_on_server), Toast.LENGTH_LONG).show()
                Log.e(TAG,"No solution???")
            }
        }
    }

    fun processSolution() {
        // loop
        do {
            if (solution==null || solution?.numberOfSteps==-1) {
                solvingInfo.throwable = NoSolutionFoundException("Solution couldn't be found. Please contact author for app improvements if the image you are trying to solve is a valid image")
                break
            }

            val extendedSolution = GameboardSolutionFinder.Companion.getExtendedSolutionPath(solution!!, solvingInfo.gameBoard!!)
            SolutionUtils.writeSolutionToLog(solution!!.numberOfSteps, extendedSolution)

            solvingInfo.solutionImage = ImageProcessing.drawSolution(solvingInfo.originalImage!!, extendedSolution!!)
            solvingInfo.stage = ImageProcessingInfo.Stage.SOLUTION
            solvingInfo.throwable = null
        } while (false)

        activity.updateImage(solvingInfo!!.getLastStageBitmap(), solvingInfo!!.stage)
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::DownloadSolutionAsyncTask"
    }
}