package com.alarmedefi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AlarmItem(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var label: String,
    var enabled: Boolean,
    var repeatDays: MutableSet<Int> = mutableSetOf()
) {
    fun repeatSummary(): String {
        if (repeatDays.isEmpty()) return "Une seule fois"
        if (repeatDays.size == 7) return "Tous les jours"
        val weekdays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        if (repeatDays == weekdays) return "Lun – Ven"
        val weekend = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (repeatDays == weekend) return "Sam – Dim"

        val order = listOf(
            Calendar.MONDAY to "Lun",
            Calendar.TUESDAY to "Mar",
            Calendar.WEDNESDAY to "Mer",
            Calendar.THURSDAY to "Jeu",
            Calendar.FRIDAY to "Ven",
            Calendar.SATURDAY to "Sam",
            Calendar.SUNDAY to "Dim"
        )
        return order.filter { it.first in repeatDays }.joinToString(", ") { it.second }
    }
}

object AlarmRepository {
    private const val PREFS = "alarme_defi_v2"
    private const val KEY_ALARMS = "alarms_json"

    fun getAll(context: Context): MutableList<AlarmItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALARMS, null) ?: return mutableListOf()
        val list = mutableListOf<AlarmItem>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val daysSet = mutableSetOf<Int>()
            val daysArr = obj.optJSONArray("repeatDays")
            if (daysArr != null) {
                for (d in 0 until daysArr.length()) {
                    daysSet.add(daysArr.getInt(d))
                }
            }
            list.add(
                AlarmItem(
                    id = obj.getInt("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    label = obj.optString("label", "Réveil"),
                    enabled = obj.getBoolean("enabled"),
                    repeatDays = daysSet
                )
            )
        }
        return list.sortedWith(compareBy({ it.hour }, { it.minute })).toMutableList()
    }

    fun saveAll(context: Context, alarms: List<AlarmItem>) {
        val arr = JSONArray()
        for (a in alarms) {
            val daysArr = JSONArray()
            for (d in a.repeatDays) {
                daysArr.put(d)
            }
            val obj = JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("enabled", a.enabled)
                put("repeatDays", daysArr)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ALARMS, arr.toString()).apply()
    }

    fun upsert(context: Context, item: AlarmItem) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) list[idx] = item else list.add(item)
        saveAll(context, list)
    }

    fun delete(context: Context, id: Int) {
        val list = getAll(context).filterNot { it.id == id }
        saveAll(context, list)
    }

    fun setEnabled(context: Context, id: Int, enabled: Boolean) {
        val list = getAll(context)
        list.find { it.id == id }?.enabled = enabled
        saveAll(context, list)
    }

    fun nextId(context: Context): Int {
        val list = getAll(context)
        return (list.maxOfOrNull { it.id } ?: 100) + 1
    }
}
