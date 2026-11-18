package com.mahi.assistant

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.hardware.display.VirtualDisplay
import java.io.File

/** MediaProjection diye screen record. Android 14-e FGS type mediaProjection lagbe. */
class ScreenRecordService : Service() {

    companion object {
        const val ACTION_STOP = "com.mahi.assistant.STOP_RECORD"

        @Volatile
        var isRecording = false
    }

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            stopSelf()
            return START_NOT_STICKY
        }

        Notifs.channel(this)
        val notif = Notifs.build(this, "MAHI screen record", "Screen record hocche...")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(2, notif)
        }

        val code = intent?.getIntExtra("code", 0) ?: 0
        val data = parcelData(intent)
        if (data == null || isRecording) {
            if (data == null) stopSelf()
            return START_NOT_STICKY
        }
        try {
            startRecording(code, data)
        } catch (e: Exception) {
            stopRecording()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun parcelData(intent: Intent?): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("data", Intent::class.java)
        else intent?.getParcelableExtra("data")

    private fun startRecording(code: Int, data: Intent) {
        val mpm = getSystemService(MediaProjectionManager::class.java)
        val mp = mpm.getMediaProjection(code, data)
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopRecording()
                stopSelf()
            }
        }, null)

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels - metrics.widthPixels % 2
        val h = metrics.heightPixels - metrics.heightPixels % 2
        val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val file = File(dir, "mahi_rec_${System.currentTimeMillis()}.mp4")

        val rec = MediaRecorder()
        rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setOutputFile(file.absolutePath)
        rec.setVideoSize(w, h)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setVideoEncodingBitRate(6_000_000)
        rec.setVideoFrameRate(30)
        rec.prepare()
        recorder = rec

        display = mp.createVirtualDisplay(
            "MAHI", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            rec.surface, null, null
        )
        rec.start()
        isRecording = true
    }

    private fun stopRecording() {
        val rec = recorder
        val disp = display
        val mp = projection
        recorder = null
        display = null
        projection = null
        isRecording = false
        try { rec?.stop() } catch (e: Exception) { }
        rec?.release()
        disp?.release()
        mp?.stop()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
