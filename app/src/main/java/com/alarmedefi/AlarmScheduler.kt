package com.alarmedefi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_ALARM_LABEL = "alarm_label"

    fun schedule(context: Context, alarm: AlarmItem) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, alarm.id, alarm.label)
        val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, pi)
        alarmManager.setAlarmClock(info, pi)
    }

    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, alarmId, ""))
    }

    fun rescheduleAllEnabled(context: Context) {
        for (alarm in AlarmRepository.getAll(context)) {
            if (alarm.enabled) {
                schedule(context, alarm)
            }
        }
    }

    private fun pendingIntent(context: Context, alarmId: Int, label: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
