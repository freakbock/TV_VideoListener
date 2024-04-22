package com.example.videolistening

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                println("BootReceiver начал работу")
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if(prefs.contains("key") && prefs.contains("period")){

                    val period = prefs.getInt("period", 15)


                    val workManager = WorkManager.getInstance(it)
                    val periodicWorkRequest = PeriodicWorkRequest.Builder(
                        BackgroundWorker::class.java,
                        period.toLong()+1,
                        TimeUnit.MINUTES,
                        period.toLong(),
                        TimeUnit.MINUTES
                    ).build()

                    val workInfos = workManager.getWorkInfosForUniqueWork("backgroundWorker").get()
                    val numberOfWorkers = workInfos.size
                    println("Запущенных WORKER: $numberOfWorkers")

                    workManager.enqueueUniquePeriodicWork(
                        "backgroundWorker",
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicWorkRequest
                    )
                    println("Worker запущен из BootReceiver")
                }
            }
        }
    }
}
