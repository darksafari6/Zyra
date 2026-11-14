package com.safari.zyra.device.control

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolAuditLogger @Inject constructor() {
    private val mutableEntries = MutableStateFlow<List<ToolAuditEntry>>(emptyList())
    val entries: StateFlow<List<ToolAuditEntry>> = mutableEntries.asStateFlow()
    fun record(entry: ToolAuditEntry) { mutableEntries.value = (listOf(entry) + mutableEntries.value).take(200) }
    fun clear() { mutableEntries.value = emptyList() }
}
