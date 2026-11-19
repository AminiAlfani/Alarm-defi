package com.alarmedefi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val ACTION_ESCALATE = "com.alarmedefi.ESCALATE"
        const val ACTION_STOP = "com.alarmedefi.STOP"
        private const val CHANNEL_ID = "alarme_defi"
        private const val NOTIF_ID = 42
        private const val AUTO_ESCALATE_MS = 20_000L
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audio: AudioManager
    private var level = 0
    private val handler = Handler(Looper.getMainLooper())

    private val autoEscalate = object : Runnable {
        override fun run() {
            raiseVolume()
            handler.postDelayed(this, AUTO_ESCALATE_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_ESCALATE -> {
                raiseVolume()
                return START_STICKY
            }
        }
        startAlarm()
        return START_STICKY
    }

    private fun startAlarm() {
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarmedefi:wake")
        wakeLock?.acquire(30 * 60 * 1000L)

        val channel = NotificationChannel(
            CHANNEL_ID, "Alarme", NotificationManager.IMPORTANCE_HIGH
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            this, 0, challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Réveil !")
            .setContentText("Résous le défi pour arrêter l'alarme")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()

        startForeground(NOTIF_ID, notification)

        level = 0
        setVolumeForLevel()
        playSound()
        handler.postDelayed(autoEscalate, AUTO_ESCALATE_MS)
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun raiseVolume() {
        level++
        setVolumeForLevel()
    }

    private fun setVolumeForLevel() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val fraction = (0.30 + level * 0.12).coerceAtMost(1.0)
        val target = (max * fraction).toInt().coerceIn(1, max)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
    }

    private fun stopEverything() {
        handler.removeCallbacks(autoEscalate)
        player?.run {
            if (isPlaying) stop()
            release()
        }
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoEscalate)
        player?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
