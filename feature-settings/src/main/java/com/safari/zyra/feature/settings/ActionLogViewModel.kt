package com.safari.zyra.feature.settings

import androidx.lifecycle.ViewModel
import com.safari.zyra.device.control.ToolAuditEntry
import com.safari.zyra.device.control.ToolAuditLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ActionLogViewModel @Inject constructor(logger: ToolAuditLogger) : ViewModel() {
    val entries: StateFlow<List<ToolAuditEntry>> = logger.entries
}
