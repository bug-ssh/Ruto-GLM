package com.rosan.ruto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.ruto.device.DeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

data class HomeUiState(
    val shizukuVersion: String = "未连接",
    val isShizukuReady: Boolean = false
)

class HomeViewModel(private val deviceManager: DeviceManager) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkShizuku()
    }

    fun checkShizuku() {
        viewModelScope.launch {
            runCatching {
                val version = Shizuku.getVersion()
                val ready = Shizuku.pingBinder()
                _uiState.update {
                    it.copy(
                        shizukuVersion = "v$version",
                        isShizukuReady = ready
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(shizukuVersion = "未连接", isShizukuReady = false)
                }
            }
        }
    }
}
