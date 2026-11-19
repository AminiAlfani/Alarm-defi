package com.alarmedefi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AlarmItem(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var label: String,
    var enabled: Boolean
)

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
            list.add(
                AlarmItem(
                    id = obj.getInt("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    label = obj.optString("label", "Réveil"),
                    enabled = obj.getBoolean("enabled")
                )
            )
        }
        return list.sortedWith(compareBy({ it.hour }, { it.minute })).toMutableList()
    }

    fun saveAll(context: Context, alarms: List<AlarmItem>) {
        val arr = JSONArray()
        for (a in alarms) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("enabled", a.enabled)
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
