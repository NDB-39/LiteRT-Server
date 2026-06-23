package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.litert.EngineManager
import com.example.metrics.MetricsManager
import com.example.server.HttpServerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isServerRunning: Boolean = false,
    val isModelLoaded: Boolean = false,
    val modelPath: String = "/storage/emulated/0/AI/models/gemma-4-e2b.litertlm", // default from prompt
    val backend: String = "CPU",
    val ramUsedMb: Long = 0L,
    val tokensPerSecond: Float = 0f,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                updateMetrics()
            }
        }
    }

    private fun updateMetrics() {
        val app = getApplication<Application>()
        val loaded = EngineManager.instance.isLoaded
        _uiState.value = _uiState.value.copy(
            isModelLoaded = loaded,
            backend = MetricsManager.backend,
            ramUsedMb = MetricsManager.getRamUsedMb(app),
            tokensPerSecond = MetricsManager.tokensPerSecond
        )
    }

    fun startServer() {
        HttpServerService.startService(getApplication())
        _uiState.value = _uiState.value.copy(isServerRunning = true)
    }

    fun stopServer() {
        HttpServerService.stopService(getApplication())
        _uiState.value = _uiState.value.copy(isServerRunning = false)
    }

    fun loadModel(path: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                EngineManager.instance.loadModel(getApplication(), path)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
