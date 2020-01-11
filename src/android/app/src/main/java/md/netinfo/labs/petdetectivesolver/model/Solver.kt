package md.netinfo.labs.petdetectivesolver.model

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log

import retrofit2.Call
import retrofit2.Response


import md.netinfo.labs.petdetectivesolver.BuildConfig
import md.netinfo.labs.petdetectivesolver.api.PDSApiClient
import md.netinfo.labs.petdetectivesolver.api.PDSApiTaskId
import md.netinfo.labs.petdetectivesolver.configuration.SolverConfiguration
import md.netinfo.labs.petdetectivesolver.consistencychecking.ConsistencyChecker
import md.netinfo.labs.petdetectivesolver.exceptions.NoSolutionFoundException
import md.netinfo.labs.petdetectivesolver.exceptions.NoValidSolutionFinderExistException
import md.netinfo.labs.petdetectivesolver.gameboard.GameBoardGenerator
import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessing
import md.netinfo.labs.petdetectivesolver.imageprocessing.ImageProcessingInfo
import md.netinfo.labs.petdetectivesolver.solutionfinder.Solution
import md.netinfo.labs.petdetectivesolver.solutionfinder.SolutionFinderFabric
import md.netinfo.labs.petdetectivesolver.utils.MemoryInfo
import md.netinfo.labs.petdetectivesolver.utils.SolutionUtils
import java.net.SocketTimeoutException


class Solver(var asyncTask: SolveAsyncTask) {
    var solvingInfo = ImageProcessingInfo(null)

    fun solve(activity: Activity, currentTask: SolveAsyncTask, image: Bitmap): ImageProcessingInfo {
        solvingInfo.originalBitmap = image

        try {
            do {
                if (BuildConfig.DEBUG) MemoryInfo.dumpMemoryInfo("before anything")

                var pds = ImageProcessing(activity, asyncTask, solvingInfo)
                pds.recognizeObjects()
                if (BuildConfig.DEBUG) MemoryInfo.dumpMemoryInfo("after object recognition")
                Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                if (currentTask.isCancelled)
                    break

                if (BuildConfig.DEBUG) MemoryInfo.dumpMemoryInfo("after image update")
                // publish progress image
                asyncTask.publishProgress(ProgressInfo(solvingInfo))
                Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                if (currentTask.isCancelled)
                    break

                val checker = ConsistencyChecker(activity)
                checker.check(pds.getFoundPets(), pds.getFoundDots())
                if (BuildConfig.DEBUG) MemoryInfo.dumpMemoryInfo("after consistency check")
                Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                if (currentTask.isCancelled)
                    break

                // generate gameboard
                val gameBoardGenerator = GameBoardGenerator(activity, pds.getFoundPets(), pds.getFoundDots())
                solvingInfo.gameBoard = gameBoardGenerator.generateArray()
                checker.check(solvingInfo.gameBoard!!)
                if (BuildConfig.DEBUG) MemoryInfo.dumpMemoryInfo("after gameboard generation")
                Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                if (currentTask.isCancelled)
                    break

                // generate simple gameboard
                solvingInfo.simpleGameBoard = solvingInfo.gameBoard!!.getSimpleGameBoardData()
                Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                if (currentTask.isCancelled)
                    break

                var solution: Solution? = null
                if(SolverConfiguration.solveRemotely) {
                    solveRemotely(solvingInfo.simpleGameBoard!!)
                } else {
                    Log.i(TAG, "Solving LOCALLY")

                    // TODO: move to settings???
                    val type = SolutionFinderFabric.Type.Dijkstra
                    var solutionFinder = SolutionFinderFabric.getSolutionFinder(type, solvingInfo.simpleGameBoard!!)
                    if (solutionFinder==null) {
                        solvingInfo.throwable = NoValidSolutionFinderExistException("There is no valid solution finder of type $type")
                        break
                    }

                    Log.i(TAG, "Task is cancelled: ${currentTask.isCancelled}")
                    if (currentTask.isCancelled)
                        break

                    val solutionMovesCount = solutionFinder.solve()
                    solution = solutionFinder.getSolution()
                    if (solution==null || solution.numberOfSteps==-1) {
                        solvingInfo.throwable = NoSolutionFoundException("Solution couldn't be found. Please contact author for app improvements if the image you are trying to solve is a valid image")
                        break
                    }

                    val extendedSolution = solutionFinder.getExtendedSolutionPath(solvingInfo.gameBoard!!)
                    SolutionUtils.writeSolutionToLog(solutionMovesCount, extendedSolution)

                    pds.drawSolution(extendedSolution!!)
                }
            } while(false)

        } catch (e: Exception) {
            solvingInfo.throwable = e
        }

        return solvingInfo
    }

    private fun solveRemotely(simpleGameBoard: SimpleGameBoardData) {
        Log.i(TAG, "Solving REMOTELY")
        val client = PDSApiClient.create()

        try {
            val call: Call<PDSApiTaskId> = client.solveChallenge(simpleGameBoard)
            var response: Response<PDSApiTaskId?> = call.execute()

            Log.i(TAG, "Solve remotely - response received")

            if (response.isSuccessful()) {
                if (response.body() != null) {
                    solvingInfo.puzzleId = response.body()!!.taskId
                    Log.i(TAG,"Solve remotely response: status OK, task id: ${solvingInfo.puzzleId}")
                } else {
                    Log.i(TAG, "Solve remotely: returned empty response")
                }
            } else {
                Log.e(TAG, "Solve remotely: status not OK")
                Log.e(TAG, "Solve remotely: error code ${response.raw().code()}")
                Log.e(TAG, "Solve remotely: error body ${response.errorBody()}")
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Socket timeout exception while connecting to the server", e)
            throw SocketTimeoutException("Failed to connect to server")
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting server", e)
            throw e
        }
    }


    companion object {
        /** TAG for logging */
        private val TAG = "PDS::Solver"
        /** What kind of image should be returned (used internally for debugging */
        const val DEBUG_GET_TEMP_IMAGE = false
    }
}
