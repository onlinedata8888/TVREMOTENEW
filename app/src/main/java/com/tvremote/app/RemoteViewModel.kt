package com.tvremote.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvremote.app.adb.AdbRemoteClient
import com.tvremote.app.adb.DiscoveredTv
import com.tvremote.app.adb.TvDiscovery
import com.tvremote.app.adb.TvPackages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR }

data class RemoteUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val discoveredTvs: List<DiscoveredTv> = emptyList(),
    val selectedTvName: String = "New TV",
    val pickerOpen: Boolean = false,
    val online: Boolean = false,
    val powerOn: Boolean = true,
    val muted: Boolean = false,
    val cursorMode: Boolean = false,
    val volumePercent: Int = 60,
    val errorMessage: String? = null
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val adb = AdbRemoteClient(app)
    private val discovery = TvDiscovery(app)

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun togglePicker() {
        _uiState.value = _uiState.value.copy(pickerOpen = !_uiState.value.pickerOpen)
        if (_uiState.value.pickerOpen) scanForTvs()
    }

    fun closePicker() {
        _uiState.value = _uiState.value.copy(pickerOpen = false)
    }

    private fun scanForTvs() {
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.SCANNING)
        viewModelScope.launch {
            val found = discovery.scan()
            _uiState.value = _uiState.value.copy(
                discoveredTvs = found,
                connectionState = if (adb.isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            )
        }
    }

    fun connectTo(tv: DiscoveredTv) {
        _uiState.value = _uiState.value.copy(
            connectionState = ConnectionState.CONNECTING,
            selectedTvName = tv.name,
            pickerOpen = false
        )
        viewModelScope.launch {
            val result = adb.connect(tv.ip)
            _uiState.value = _uiState.value.copy(
                connectionState = if (result.isSuccess) ConnectionState.CONNECTED else ConnectionState.ERROR,
                online = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun togglePower() = act {
        adb.power()
        _uiState.value = _uiState.value.copy(powerOn = !_uiState.value.powerOn)
    }

    fun toggleMute() = act {
        adb.mute()
        _uiState.value = _uiState.value.copy(muted = !_uiState.value.muted)
    }

    fun toggleCursorMode() {
        _uiState.value = _uiState.value.copy(cursorMode = !_uiState.value.cursorMode)
    }

    fun setVolume(percent: Int) = act {
        _uiState.value = _uiState.value.copy(volumePercent = percent)
        adb.setVolumePercent(percent)
    }

    fun stepVolume(delta: Int) = setVolume((_uiState.value.volumePercent + delta).coerceIn(0, 100))

    fun back() = act { adb.back() }
    fun home() = act { adb.home() }
    fun recentApps() = act { adb.recentApps() }
    fun assistant() = act { adb.assistant() }
    fun menu() = act { adb.menu() }
    fun openSettings() = act { adb.openSettings() }
    fun openKeyboard() = act { adb.openKeyboard() }

    fun dpadTap() = act { adb.dpadCenter() }
    fun dpadDirection(dx: Int, dy: Int) = act {
        when {
            kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 0 -> adb.dpadRight()
            kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx < 0 -> adb.dpadLeft()
            dy > 0 -> adb.dpadDown()
            dy < 0 -> adb.dpadUp()
        }
    }

    fun launchYoutube() = act { adb.launchApp(TvPackages.YOUTUBE) }
    fun launchNetflix() = act { adb.launchApp(TvPackages.NETFLIX) }
    fun launchPrime() = act { adb.launchApp(TvPackages.PRIME_VIDEO) }

    /** Runs an ADB action only if connected; silently no-ops otherwise (mirrors a real remote). */
    private fun act(block: suspend () -> Unit) {
        if (!adb.isConnected) return
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        super.onCleared()
        adb.disconnect()
    }
}
