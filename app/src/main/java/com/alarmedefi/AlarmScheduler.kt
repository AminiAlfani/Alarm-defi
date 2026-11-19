package com.alarmedefi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_ALARM_LABEL = "alarm_label"

    fun computeNextTriggerMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            if (base.timeInMillis <= nowMillis) {
                base.add(Calendar.DAY_OF_YEAR, 1)
            }
            return base.timeInMillis
        }

        for (offset in 0..7) {
            val candidate = (base.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in alarm.repeatDays && candidate.timeInMillis > nowMillis) {
                return candidate.timeInMillis
            }
        }

        base.add(Calendar.DAY_OF_YEAR, 1)
        return base.timeInMillis
    }

    fun schedule(context: Context, alarm: AlarmItem) {
        val triggerAt = computeNextTriggerMillis(alarm)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, alarm.id, alarm.label)
        val info = AlarmManager.AlarmClockInfo(triggerAt, pi)
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
