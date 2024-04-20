package com.example.videolistening

import DBHelper
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.videolistening.api.ListVideoAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var listVideoAPI: ListVideoAPI
    lateinit var dbHelper: DBHelper

    lateinit var key: EditText
    lateinit var period: EditText
    lateinit var surfaceView: SurfaceView
    lateinit var background_white: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mediaPlayer = MediaPlayer()
        dbHelper = DBHelper(this)
        listVideoAPI = ListVideoAPI(applicationContext)
        key = findViewById(R.id.key)
        period = findViewById(R.id.period)
        background_white = findViewById(R.id.background_white)

        surfaceView = findViewById(R.id.surfaceView)
//        val surfaceHolder = surfaceView.holder
//        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {mediaPlayer.setDisplay(holder) }
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {mediaPlayer.setDisplay(holder)}
//            override fun surfaceDestroyed(holder: SurfaceHolder) { mediaPlayer.setDisplay(null) }
//        })


        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if(prefs.contains("key") && prefs.contains("period")){
            StartTimerOnGETQuery(prefs.getInt("period", 12))

            surfaceView.visibility = View.VISIBLE

            key.setText(prefs.getString("key", ""))
            period.setText(prefs.getInt("period", 12).toString())
            videoNames = dbHelper.getAllVideoNames()
            if(videoNames.size == 0){
                getVideoLinksAPI(prefs.getString("key", "").toString(), prefs.getInt("period", 1).toString())
            }
            else{
                playVideoAtIndex(0)
            }
        }
    }

    fun StartTimerOnGETQuery(period: Int){
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWork("backgroundWorker").get()
            .forEach{
                workInfo ->
                if(workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED){
                    return
                }
            }

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            BackgroundWorker::class.java,
            period.toLong(),
            TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork("backgroundWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest)
    }

    fun getVideoLinksAPI(keyText: String, periodText: String){
        CoroutineScope(Dispatchers.IO).launch {
            println("Работа с API началась")
            listVideoAPI.getLinks(keyText)
            val deferred = async {
                dbHelper.getAllVideoNames()
            }
            videoNames = deferred.await()

            withContext(Dispatchers.Main) {
                println("Запускаем первое видео")
                StartTimerOnGETQuery(periodText.toInt())
                playVideoAtIndex(0)
            }
        }
    }

    fun SaveConfig(view: View){
        try{
            var keyText = key.text.toString()
            var periodText = period.text.toString()
            if(keyText.isNotEmpty() && periodText.isNotEmpty())
            {
                surfaceView.visibility = View.VISIBLE

                val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("key", keyText)
                editor.putInt("period", periodText.toInt())
                editor.apply()

                getVideoLinksAPI(keyText, periodText)
            }
            else
                Toast.makeText(this, "Заполните поле", Toast.LENGTH_SHORT)
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoNames: List<String>
    private var isNullVideo: Boolean = false


    private fun playVideoAtIndex(index: Int) {
        if (index < videoNames.size && videoNames.size!=0) {
            try {
                println("Включаем player")
                val path = File(filesDir, videoNames[index]).path
                mediaPlayer.apply {
                    reset()
                    setDataSource(path)
                    prepareAsync()
                    setOnPreparedListener {

                        surfaceView.holder.surface.release()
                        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {setDisplay(holder)}
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) {setDisplay(null)}
                        })

                        // Воспроизводим видео только после его подготовки
                        println("ЗАПУСКАЕМ ВИДЕО ${videoNames[index]}")
                        start()
                    }
                    setOnCompletionListener {
                        println("ВИДЕО ПРОИГРАНО")

                        android.os.Handler().postDelayed({
                            if (isVideoListUpdated()) {
                                playVideoAtIndex(0)
                            } else {
                                playVideoAtIndex(index + 1)
                            }
                        }, 1000)
                    }

                    setOnErrorListener { mp, what, extra ->
                        println("ОШИБКА ПРИ ПРОИГРЫВАНИИ ВИДЕО")
                        playVideoAtIndex(index + 1)
                        true
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка воспроизведения видео", Toast.LENGTH_SHORT).show()
                if(videoNames.size > index)
                {
                    playVideoAtIndex(index + 1)
                }
                else{
                    playVideoAtIndex(0)
                }
            }
        } else if(videoNames.size !=0){
            playVideoAtIndex(0)
        }
    }

    fun isVideoListUpdated(): Boolean{
        val videoNamesUpdate = dbHelper.getAllVideoNames()
        if(videoNamesUpdate.equals(videoNames)){
            return false
        }
        else{
            videoNames = videoNamesUpdate
            return true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release() // Освободить ресурсы MediaPlayer при уничтожении активности
    }

}