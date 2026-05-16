package com.example.flashlearn.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ReportApiService {
    @GET("reports/stats")
    suspend fun downloadStatsPdf(
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}