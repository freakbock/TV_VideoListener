package com.example.videolistening.api

import android.content.Context
import android.util.Log
import com.example.videolistening.Config
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VideoManager(context: Context) {
    private val apiService: ApiService
    private var context: Context
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(Config.URL + "api/getFile/")
            .build()

        apiService = retrofit.create(ApiService::class.java)
        this.context = context
    }

    fun downloadVideo(videoFileName: String, listener: VideoDownloadListener) {
        try{
            val videoUrl = videoFileName
            val outputFile = File(context.filesDir, videoFileName)

            val call = apiService.downloadVideo(videoUrl)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val success = writeResponseBodyToDisk(response.body(), outputFile)
                        if (success) {
                            listener.onDownloadComplete(outputFile)
                            println("Видео загружено: $videoFileName")
                        } else {
                            listener.onDownloadFailed()
                            println("Видео не удалось загрузить: $videoFileName")
                        }
                    } else {
                        listener.onDownloadFailed()
                        println("Видео не удалось загрузить: $videoFileName")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    listener.onDownloadFailed()
                }
            })
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun writeResponseBodyToDisk(body: ResponseBody?, outputFile: File): Boolean {
        body ?: return false
        return try {
            val fos = FileOutputStream(outputFile)
            fos.write(body.bytes())
            fos.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    interface VideoDownloadListener {
        fun onDownloadComplete(videoFile: File)
        fun onDownloadFailed()
    }

    fun deleteFile(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        println("Видео удалено: $fileName")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
