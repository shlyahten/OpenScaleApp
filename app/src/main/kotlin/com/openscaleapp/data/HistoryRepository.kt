package com.openscaleapp.data

import com.openscaleapp.model.WeighInRecord
import kotlinx.coroutines.flow.StateFlow

interface HistoryRepository {
    val history: StateFlow<List<WeighInRecord>>
    fun saveRecord(record: WeighInRecord)
    fun deleteRecord(id: String)
    fun clearHistory()
}
