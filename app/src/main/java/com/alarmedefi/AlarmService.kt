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
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val ACTION_ESCALATE = "com.alarmedefi.ESCALATE"
        const val ACTION_STOP = "com.alarmedefi.STOP"
        private const val CHANNEL_ID = "alarme_defi_max"
        private const val NOTIF_ID = 42
        private const val VOLUME_LOCK_INTERVAL_MS = 400L
        private const val SIREN_PULSE_MS = 900L
    }

    private var player: MediaPlayer? = null
    private var toneGen: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audio: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentAlarmId: Int = -1
    private var currentLabel: String = "RÉVEIL !"
    private var sirenHigh = true

    private val volumeLockLoop = object : Runnable {
        override fun run() {
            forceMaxVolume()
            handler.postDelayed(this, VOLUME_LOCK_INTERVAL_MS)
        }
    }

    private val sirenPulseLoop = object : Runnable {
        override fun run() {
            try {
                val tone = if (sirenHigh) ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK else ToneGenerator.TONE_SUP_ERROR
                toneGen?.startTone(tone, 700)
                sirenHigh = !sirenHigh
            } catch (_: Exception) {
            }
            handler.postDelayed(this, SIREN_PULSE_MS)
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
                forceMaxVolume()
                triggerHarshVibration()
                return START_STICKY
            }
        }
        currentAlarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        currentLabel = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "RÉVEIL !"
        startAlarm()
        return START_STICKY
    }

    private fun startAlarm() {
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarmedefi:wake")
        wakeLock?.acquire(45 * 60 * 1000L)

        val channel = NotificationChannel(
            CHANNEL_ID, "Alarme Maximale", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, currentLabel)
        }
        val pending = PendingIntent.getActivity(
            this, currentAlarmId.coerceAtLeast(0), challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(currentLabel)
            .setContentText("Résous le défi pour couper l'alarme !")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()

        startForeground(NOTIF_ID, notification)

        forceMaxVolume()
        playMaxNoise()
        triggerHarshVibration()

        handler.removeCallbacks(volumeLockLoop)
        handler.post(volumeLockLoop)

        handler.removeCallbacks(sirenPulseLoop)
        handler.post(sirenPulseLoop)
    }

    private fun forceMaxVolume() {
        try {
            val streams = intArrayOf(
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING
            )
            for (stream in streams) {
                val max = audio.getStreamMaxVolume(stream)
                if (audio.getStreamVolume(stream) < max) {
                    audio.setStreamVolume(stream, max, 0)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun playMaxNoise() {
        try {
            player?.release()
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
                setVolume(1.0f, 1.0f)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }

        try {
            toneGen?.release()
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (_: Exception) {
        }
    }

    private fun triggerHarshVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 600, 150, 600, 150, 900)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopEverything() {
        handler.removeCallbacks(volumeLockLoop)
        handler.removeCallbacks(sirenPulseLoop)
        player?.run {
            if (isPlaying) stop()
            release()
        }
        player = null
        try {
            toneGen?.stopTone()
            toneGen?.release()
        } catch (_: Exception) {
        }
        toneGen = null
        vibrator?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(volumeLockLoop)
        handler.removeCallbacks(sirenPulseLoop)
        player?.release()
        toneGen?.release()
        vibrator?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
