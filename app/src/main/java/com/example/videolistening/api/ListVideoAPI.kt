package com.example.videolistening.api

import DBHelper
import android.content.Context
import android.util.Log
import com.example.videolistening.Config
import com.google.gson.Gson
import okhttp3.*
import retrofit2.Retrofit
import java.io.File
import java.io.IOException

class ListVideoAPI(private val context: Context) {
    private val client = OkHttpClient()

    private val apiService: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(Config.URL)
            .build()

        retrofit.create(ApiService::class.java)
    }

    fun getLinks(key: String) {
        println("Делаем запрос к API")
        try {
            val dbHelper = DBHelper(context)
            val videoDownloader = VideoManager(context)
            val listener = object : VideoManager.VideoDownloadListener {
                override fun onDownloadComplete(videoFile: File) {
                    println("Видео загружено")
                }

                override fun onDownloadFailed() {
                    println("Ошибка загрузки видео")
                }
            }

            val apiUrl = "/api/getStand/$key"

            apiService.getListVideos(apiUrl).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onFailure(call: retrofit2.Call<ResponseBody>, e: Throwable) {
                    println("Ошибка запроса: ${e.message}")
                }

                override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        println("Запрос не выполнен: ${response.code()}")
                        return
                    }
                    println("Успешный запрос и получение ответа")
                    val responseBody = response.body()?.string()
                    val gson = Gson()
                    val videoNames: List<String> = gson.fromJson(responseBody, Array<String>::class.java).toList()

                    val videoNamesSave = dbHelper.getAllVideoNames()

                    val onlyInVideoNamesSave = findNamesOnlyInVideoNamesSave(videoNames, videoNamesSave)
                    val onlyInVideoNames = findNamesOnlyInVideoNames(videoNames, videoNamesSave)

                    if (onlyInVideoNames.isNotEmpty()) {
                        println("Есть новые видео")
                        for (videoName in onlyInVideoNames) {
                            dbHelper.insertVideoName(videoName)
                            videoDownloader.downloadVideo(videoName, listener)
                        }
                    }
                    if (onlyInVideoNamesSave.isNotEmpty()) {
                        println("Есть видео, которые необходимо удалить")
                        for (videoName in onlyInVideoNamesSave) {
                            dbHelper.deleteVideoName(videoName)
                            videoDownloader.deleteFile(videoName)
                        }
                    }
                    dbHelper.close()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun findNamesOnlyInVideoNamesSave(videoNames: List<String>, videoNamesSave: List<String>): List<String> {
        return videoNamesSave.subtract(videoNames).toList()
    }

    private fun findNamesOnlyInVideoNames(videoNames: List<String>, videoNamesSave: List<String>): List<String> {
        return videoNames.subtract(videoNamesSave).toList()
    }
}
