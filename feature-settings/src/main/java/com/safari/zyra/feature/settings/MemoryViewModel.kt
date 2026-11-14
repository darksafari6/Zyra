package com.safari.zyra.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safari.zyra.core.data.memory.MemoryFact
import com.safari.zyra.core.data.memory.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MemoryViewModel @Inject constructor(private val repository: MemoryRepository) : ViewModel() {
    private val mutableFacts = MutableStateFlow<List<MemoryFact>>(emptyList())
    val facts: StateFlow<List<MemoryFact>> = mutableFacts.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError.asStateFlow()
    init { viewModelScope.launch { repository.facts.collect { mutableFacts.value = it } } }
    fun save(key: String, value: String) {
        viewModelScope.launch {
            runCatching { repository.remember(key, value) }
                .onFailure { mutableError.value = it.message ?: "Could not save memory." }
                .onSuccess { mutableError.value = null }
        }
    }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id) } }
    fun export(): String = mutableFacts.value.joinToString("\n") { "${it.factKey}: ${it.factValue}" }
    fun clearError() { mutableError.value = null }
}
