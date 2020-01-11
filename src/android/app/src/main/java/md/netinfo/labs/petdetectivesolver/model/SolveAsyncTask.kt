package md.netinfo.labs.petdetectivesolver.model

import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast

import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessingInfo
import md.netinfo.labs.petdetectivesolver.view.MainActivity

class SolveAsyncTask(parentActivity: MainActivity, currentImage: Bitmap): AsyncTask<Void, ProgressInfo, Void>() {
    private var activity: MainActivity = parentActivity
    private var image: Bitmap? = currentImage

    override fun onPreExecute() {
        Log.i(TAG, "onPostExecute: activity = $activity")

        // reset image processing info
        activity.imageProcessingResult = ImageProcessingInfo(image)
        activity.updateMenuItems()

        // show original image
        activity.updateImage(image, 0)
        activity.solvingTask = true
        activity.solvedTask = false
    }

    override fun doInBackground(vararg params: Void): Void? {
        Log.d(TAG, "doInBackground - start")
        var pds = Solver(this)

        try {
            activity.imageProcessingResult = pds.solve(activity, this, image!!)
        } catch (e: Exception) {
            activity.imageProcessingResult!!.throwable = e
            Log.e(TAG, "Exception occured while processing current image", e)
            image = activity.imageProcessingResult!!.getLastStageBitmap()
        }

        Log.d(TAG, "doInBackground - before return")
        return null
    }

    override fun onPostExecute(tmp: Void?) {
        Log.i(TAG, "onPostExecute: activity = $activity")
        activity.solvingTask = false
        activity.solvedTask = true

        Log.i(TAG, "onPostExecute started")

        activity.updateImage(activity.imageProcessingResult!!.getLastStageBitmap(), activity.imageProcessingResult!!.stage);
        activity.showProcessingErrorMessage()
    }

    override fun onProgressUpdate(vararg values: ProgressInfo?) {
        try {
            super.onProgressUpdate(*values)
            val value: ProgressInfo = values[0]!!
            if (value.info!=null) {
                activity.updateImage(value.info.getLastStageBitmap(), value.info.stage)
            }
            // asclearuc:TODO Toast works too slowly
            if (value.toastMessage!=null) {
                Toast.makeText(activity, value.toastMessage, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occured while updating progress", e)
        }
    }

    fun publishProgress(progress: ProgressInfo) {
        super.publishProgress(progress)
    }

    override fun onCancelled() {
        super.onCancelled()
        Log.i(TAG, "Solver task cancelled")
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::SolveAsyncTask"
    }
}