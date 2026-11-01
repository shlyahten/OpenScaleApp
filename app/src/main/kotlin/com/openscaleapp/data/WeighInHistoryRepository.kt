package com.openscaleapp.data

import android.content.Context
import android.util.Log
import com.openscaleapp.model.WeighInRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class WeighInHistoryRepository(context: Context) : HistoryRepository {
    private val prefs = context.getSharedPreferences("chipsea_weigh_in_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(loadHistory())
    override val history: StateFlow<List<WeighInRecord>> = _history.asStateFlow()

    private fun loadHistory(): List<WeighInRecord> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<WeighInRecord>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(WeighInRecord.fromJson(item))
            }
            list.sortedByDescending { it.timestampMs }
        } catch (e: Exception) {
            Log.e("WeighInHistoryRepo", "Failed to parse history JSON", e)
            emptyList()
        }
    }

    private fun persist(list: List<WeighInRecord>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        _history.value = list
    }

    @Synchronized
    override fun saveRecord(record: WeighInRecord) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == record.id }
        current.add(0, record)
        persist(current.sortedByDescending { it.timestampMs })
    }

    @Synchronized
    override fun deleteRecord(id: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == id }
        persist(current)
    }

    @Synchronized
    override fun clearHistory() {
        persist(emptyList())
    }

    companion object {
        private const val KEY_HISTORY = "history_records_json"
    }
}
