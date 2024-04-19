package com.example.videolistening.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    fun downloadVideo(@Url fileUrl: String): Call<ResponseBody>

    @GET
    fun getListVideos(@Url url: String): Call<ResponseBody>
}
