package md.netinfo.labs.petdetectivesolver.api

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Call

import md.netinfo.labs.petdetectivesolver.configuration.SolverConfiguration
import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData

interface PDSApiClient {

    @POST("pds/solve")
    fun solveChallenge(@Body gbd: SimpleGameBoardData): Call<PDSApiTaskId>

    @POST("pds/solveText")
    fun solveChallengeText(@Body gbd: SimpleGameBoardData): Call<String>

    @GET("pds/getSolution/{id}")
    fun downloadSolution(@Path("id") id: String) : Call<PDSApiSolution>

    companion object {

        fun create(): PDSApiClient {

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(SolverConfiguration.remoteUrl)
                .build()

            return retrofit.create(PDSApiClient::class.java)
        }
    }
}