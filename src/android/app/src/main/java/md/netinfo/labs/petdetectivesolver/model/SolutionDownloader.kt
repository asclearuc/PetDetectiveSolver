package md.netinfo.labs.petdetectivesolver.model

import android.util.Log
import retrofit2.Call
import retrofit2.Response
import java.net.SocketTimeoutException

import md.netinfo.labs.petdetectivesolver.api.PDSApiClient
import md.netinfo.labs.petdetectivesolver.api.PDSApiSolution
import md.netinfo.labs.petdetectivesolver.solutionfinder.Solution
import java.net.SocketException

class SolutionDownloader {

    fun downloadSolution(id: String) : Solution? {
        Log.i(TAG, "Checking remote site for solution for '$id'")
        val client = PDSApiClient.create()

        var solution: Solution? = null

        try {
            val call: Call<PDSApiSolution> = client.downloadSolution(id)
            var response: Response<PDSApiSolution?> = call.execute()

            Log.i(TAG, "Response received")

            if (response.isSuccessful()) {
                if (response.body() != null) {
                    solution = response.body()!!.solution
                    Log.i(TAG, "Rresponse: status OK"
                    )
                } else {
                    Log.i(TAG, "Received empty response")
                }
            } else {
                Log.e(TAG, "Status not OK")
                Log.e(TAG, "Error body ${response.raw().code()}")
                Log.e(TAG, "Error body ${response.errorBody()}")
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Socket exception while connecting to the server", e)
            throw SocketTimeoutException("Failed to connect to server")
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Socket timeout exception while connecting to the server", e)
            throw SocketTimeoutException("Failed to connect to server")
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting server", e)
            throw e
        }

        return solution
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::SolutionDownloader"
        /** What kind of image should be returned (used internally for debugging */
        const val DEBUG_GET_TEMP_IMAGE = false
    }
}