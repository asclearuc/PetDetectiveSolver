package md.netinfo.labs.petdetectivesolver.view

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_fullscreen.*
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat

import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.BaseLoaderCallback
import org.opencv.core.Core

import md.netinfo.labs.petdetectivesolver.R
import md.netinfo.labs.petdetectivesolver.model.SampleImageStorage
import md.netinfo.labs.petdetectivesolver.resources.GameObjects
import md.netinfo.labs.petdetectivesolver.configuration.SolverConfiguration
import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessingInfo
import md.netinfo.labs.petdetectivesolver.model.DownloadSolutionAsyncTask
import md.netinfo.labs.petdetectivesolver.model.SolveAsyncTask
import md.netinfo.labs.petdetectivesolver.utils.MemoryInfo


/**
 * An full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class MainActivity : AppCompatActivity(), ComponentCallbacks2 {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    private var appMenu: Menu? = null
    private var sampleImageStorage: SampleImageStorage? = null
    private var currentImage: Bitmap? = null
    private var currentImageOriginal: Bitmap? = null
    internal var solvingTask: Boolean = false
    internal var solvedTask: Boolean = false
    // stores info about different stages of image processing
    var imageProcessingResult: ImageProcessingInfo? = null
    // tasks
    private var solverTask: SolveAsyncTask? = null

    // callback to init OpenCV
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS ->                          { Log.i(TAG, "OpenCV loaded successfully, version ${Core.VERSION}") }
                LoaderCallbackInterface.INIT_FAILED ->                      { Log.i(TAG,"Init Failed") }
                LoaderCallbackInterface.INSTALL_CANCELED ->                 { Log.i(TAG,"Install Cancelled") }
                LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION ->     { Log.i(TAG,"Incompatible Version") }
                LoaderCallbackInterface.MARKET_ERROR ->                     { Log.i(TAG,"Market Error") }
                else -> { super.onManagerConnected(status) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate method called")
        Log.d(TAG, "onCreate: activity=$this, solving=$solvingTask solved=$solvedTask")

//        // init OpenCV
//        val loadStatus = OpenCVLoader.initDebug()
//        Log.d(TAG, "OpenCV load library status: $loadStatus")

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        GameObjects.init(applicationContext)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        solve_button.setOnTouchListener(mDelayHideTouchListener)

        // init sample image store
        sampleImageStorage = SampleImageStorage(applicationContext)

        // acquire permissions if needed
        acquirePermissions()

        when {
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                handleSendImage(intent)
            }
        }

        // on click listener
        solve_button.setOnClickListener {
            // asclearuc:TODO
            // May be to refactor to advanced 'when' ?
            if (currentImage == null) {
                Log.i(TAG, getString(R.string.no_image_loaded))
                Toast.makeText(applicationContext, getString(R.string.no_image_loaded), Toast.LENGTH_SHORT).show()
            } else if(solvingTask == true) {
                Log.i(TAG, getString(R.string.task_is_running))
                Toast.makeText(applicationContext, getString(R.string.task_is_running), Toast.LENGTH_SHORT).show()
            } else if (solvedTask == true) {
                Log.i(TAG, getString(R.string.task_already_solved))
                Toast.makeText(applicationContext, getString(R.string.task_already_solved), Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, getString(R.string.task_starting))
                currentImage = currentImageOriginal
                solverTask = SolveAsyncTask(this, currentImage!!)
                solverTask!!.execute()
                Toast.makeText(applicationContext, getString(R.string.task_starting), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(AUTO_HIDE_INITIAL_DELAY_MILLIS)
    }

    /**
     * Function onStart. Does nothing.
     */
    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart method called")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)

        // save menu to access it later
        appMenu = menu!!

        // check solve locally/remotely flag
        menu!!.findItem(R.id.solve_remotely).isChecked = SolverConfiguration.solveRemotely

        updateMenuItems()

        return true
    }

    /**
     * Function onResume. Makes an attempt to load OpenCV library.
     */
    override fun onResume() {
        super.onResume()

        Log.i(TAG, "onResume method called")
        Log.d(TAG, "onResume: solving=$solvingTask solved=$solvedTask")

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization" )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    /**
     * Function onPostResume. Does nothing.
     */
    override fun onPostResume() {
        super.onPostResume()
        Log.i(TAG, "onPostResume method called")
    }

    /**
     * Function onPause. Does nothing.
     */
    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause method called")
    }

    /**
     * Function onStop. Does nothing.
     */
    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop method called")
    }

    /**
     * Function onDestroy. Does nothing.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy method called")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.load_sample -> { onOptionItemSelectLoadSample() }
            R.id.clear_image -> { onOptionsItemSelectedClearImage() }
            R.id.solve_remotely -> {
                SolverConfiguration.solveRemotely = !SolverConfiguration.solveRemotely
                item.isChecked = SolverConfiguration.solveRemotely
                true
            }
            R.id.check_server           -> { checkServerForSolution() }
            R.id.show_original_image    -> { updateImage(imageProcessingResult!!::getOriginalImage,      ImageProcessingInfo.Stage.ORIGINAL) }
            R.id.show_resized_image     -> { updateImage(imageProcessingResult!!::getResizedImage,       ImageProcessingInfo.Stage.RESIZED) }
            R.id.show_cropped_image     -> { updateImage(imageProcessingResult!!::getCroppedImage,       ImageProcessingInfo.Stage.CROPPED) }
            R.id.show_grayscaled_image  -> { updateImage(imageProcessingResult!!::getGrayscaledImage,    ImageProcessingInfo.Stage.GRAYSCALE) }
            R.id.show_canny_image       -> { updateImage(imageProcessingResult!!::getCannyImage,         ImageProcessingInfo.Stage.CANNY) }
            R.id.show_dots_with_roads   -> { updateImage(imageProcessingResult!!::getDotsWithRoadsImage, ImageProcessingInfo.Stage.DOTSWITHROADS) }
            R.id.show_detected_objects_image   -> { updateImage(imageProcessingResult!!::getDetectedObjectsImage, ImageProcessingInfo.Stage.DETECTEDOBJECTS) }
            R.id.show_solution_image    -> { updateImage(imageProcessingResult!!::getSolutionImage,      ImageProcessingInfo.Stage.SOLUTION) }
            R.id.show_processing_error  -> {
                showProcessingErrorMessage()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     *
     */
    private fun acquirePermissions() {
        if (!hasPermissions())
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE)
    }

    /**
     * Checks if application has required permissions
     * @return True if application has required permissions
     */
    private fun hasPermissions(): Boolean {
        var result = true
        for (perm in PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            if (!result)
                break
        }
        return result
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS_CODE -> {
                if (hasPermissions())
                    Log.i(TAG, "Permissions granted")
                else
                    Log.w(TAG, "No permissions")
            }
            else                    -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


    /**
     * Check if solution for current puzzle is available on server
     * @return True always
     */
    private fun checkServerForSolution(): Boolean {
        if (imageProcessingResult==null ||  imageProcessingResult!!.puzzleId=="") {
            Toast.makeText(applicationContext, getString(R.string.puzzle_not_sent), Toast.LENGTH_LONG).show()
            return true
        }

        DownloadSolutionAsyncTask(this).execute()
        return true
    }

    /**
     * Logic on Load Sample menu item is selected
     */
    private fun onOptionItemSelectLoadSample(): Boolean {
        val imgStor = sampleImageStorage?.getRandomImage()
        Log.d(TAG, "Loading asset image '${imgStor?.name}'")
        val updateStatus = updateImage(imgStor?.bitmap, R.string.load_image_while_solving_task)
        Toast.makeText(
            applicationContext,
            getString(R.string.toast_load_random_image, imgStor?.name),
            Toast.LENGTH_SHORT
        ).show()
        Log.d(TAG, "Load sample menu item selected")
        if (updateStatus == true) {
            currentImageOriginal = imgStor?.bitmap
            solvedTask = false
        }
        // result previous results (if any)
        imageProcessingResult = null
        updateMenuItems()

        return true
    }

    /**
     * Logic on Clear Image menu item is selected
     */
    private fun onOptionsItemSelectedClearImage(): Boolean {
        val updateStatus = updateImage(null, R.string.clear_image_while_solving_task)
        if (updateStatus == true) {
            // reset image processing information
            imageProcessingResult = null
            solvedTask = false
            updateMenuItems()
        }
        return true
    }

    /**
     * Update image. Image can be updated only if there is no process that solves current image
     * @param img Image to show
     * @param resId String resource id to display in case of error
     * @return True if image was updated, otherwise false
     */
    internal fun updateImage(img: Bitmap?, resId: Int):Boolean {
        if (solvingTask == false) {
            currentImage = img
            fullscreen_content.setImageBitmap(currentImage)
            return true
        }
        // there is a solving task, do not update the image
        if (resId!=0) {
            // show warning message
            Log.w(TAG, getString(resId))
            Toast.makeText(applicationContext, getString(resId), Toast.LENGTH_SHORT).show()
        } else {
            // update screen
            fullscreen_content.setImageBitmap(img)
        }
        return false
    }

    internal fun updateImage(img: Bitmap?, stage: ImageProcessingInfo.Stage)
    {
        updateImage(img, 0)
        updateMenuItems()
        Toast.makeText(applicationContext, getString(R.string.toast_showing_stage_image, stage.name), Toast.LENGTH_SHORT).show()
    }
    /**
     * Display image from processing information according to paramaters
     * @param function Specify what image to select
     * @param stage Specify what stage to show
     * @return Always true
     */
    private fun updateImage(function: () -> Bitmap?, stage: ImageProcessingInfo.Stage): Boolean {
        if (imageProcessingResult == null) {
            Toast.makeText(applicationContext, getString(R.string.toast_processing_info_not_initialized), Toast.LENGTH_SHORT).show()
            return true
        }
        val bitmap = function()
        if (bitmap != null) {
            fullscreen_content.setImageBitmap(bitmap)
            Toast.makeText(applicationContext, getString(R.string.toast_showing_stage_image, stage.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, getString(R.string.toast_no_stage_image, stage.name), Toast.LENGTH_SHORT).show()
        }
        return true
    }

    internal fun showProcessingErrorMessage() {
        if (imageProcessingResult?.throwable==null) {
            Toast.makeText(applicationContext, getString(R.string.toast_no_errors), Toast.LENGTH_LONG).show()
        } else {
            var builder: AlertDialog.Builder? = null
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            } else {
                builder = AlertDialog.Builder(this)
            }
            val excMsg = imageProcessingResult!!.throwable!!.message
            builder!!.setTitle("Error occured")
                .setMessage(String.format(getString(R.string.error_dialog, excMsg, imageProcessingResult!!.stage.name)))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    /**
     * Function to update enable/disable menu items.
     * Menu items are enabled if there is image processing info
     */
    internal fun updateMenuItems() {
        val flag: Boolean = (imageProcessingResult != null && !solvingTask && solvedTask)
        appMenu?.findItem(R.id.show_original_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_resized_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_cropped_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_grayscaled_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_dots_with_roads)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_canny_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_detected_objects_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_solution_image)?.setEnabled(flag)
        appMenu?.findItem(R.id.show_processing_error)?.setEnabled(flag)

    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun handleSendImage(intent: Intent) {
        try {
            (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let {
                Log.d(TAG, "Uri is $it")
                val inputStream = contentResolver.openInputStream(it)
                Log.d(TAG, "InputStream $inputStream")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                Log.d(TAG, "Bitmap: $bitmap")
                if(updateImage(bitmap, 0)) {
                    solverTask?.cancel(true)
                    currentImageOriginal = currentImage
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming image", e)
            Toast.makeText(applicationContext, getString(R.string.error_parsing_image), Toast.LENGTH_LONG).show()
        }
    }

    // ComponentCallbacks2 part
    override fun onTrimMemory(level: Int) {
        // Determine which lifecycle or system event was raised.
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN       -> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_UI_HIDDEN") }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE-> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_RUNNING_MODERATE") }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW     -> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_RUNNING_LOW") }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL-> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_RUNNING_CRITICAL") }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND      -> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_BACKGROUND") }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE        -> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_MODERATE") }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE        -> { Log.i(MemoryInfo.getMemoryInfoTag(), "onTrimMemory:TRIM_MEMORY_COMPLETE") }
        }
    }
    override fun onLowMemory() {
        super.onLowMemory()
        Log.i(MemoryInfo.getMemoryInfoTag(), "onLowMemory")
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * Trigger the initial hide() shortly after the activity has been
         * created, to briefly hint to the user that UI controls are available.
         */
        private val AUTO_HIDE_INITIAL_DELAY_MILLIS = 1000
        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
        /**
         * Permissions for the application
         */
        private val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        /**
         *
         */
        private val REQUEST_PERMISSIONS_CODE = 0
        /** TAG for logging */
        private val TAG = "PDS::MainActivity"
    }
}
