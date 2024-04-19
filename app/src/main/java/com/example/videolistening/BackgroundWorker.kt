package com.example.videolistening

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.videolistening.api.ListVideoAPI


class BackgroundWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {

        CheckNewVideos()

        return Result.success()
    }

    fun CheckNewVideos(){
        try{
            val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val key = prefs.getString("key", "").toString()

            val listVideoAPI = ListVideoAPI(applicationContext)
            listVideoAPI.getLinks(key)
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }
}