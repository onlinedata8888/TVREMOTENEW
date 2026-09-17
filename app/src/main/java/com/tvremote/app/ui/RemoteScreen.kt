package com.tvremote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvremote.app.ConnectionState
import com.tvremote.app.RemoteViewModel
import com.tvremote.app.ui.components.AppsRow
import com.tvremote.app.ui.components.BottomControls
import com.tvremote.app.ui.components.TopBar
import com.tvremote.app.ui.components.Touchpad
import com.tvremote.app.ui.components.VolumeSlider
import com.tvremote.app.ui.theme.RemoteColors

@Composable
fun RemoteScreen(viewModel: RemoteViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(listOf(RemoteColors.ScreenBg1, RemoteColors.ScreenBg2))
            )
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TopBar(
            powerOn = state.powerOn,
            onPowerClick = viewModel::togglePower,
            online = state.online,
            tvName = state.selectedTvName,
            pickerOpen = state.pickerOpen,
            discoveredTvs = state.discoveredTvs,
            onTogglePicker = viewModel::togglePicker,
            onSelectTv = viewModel::connectTo,
            onMenuClick = viewModel::menu
        )

        if (state.connectionState == ConnectionState.DISCONNECTED || state.connectionState == ConnectionState.SCANNING) {
            Text(
                text = if (state.connectionState == ConnectionState.SCANNING)
                    "Scanning your Wi-Fi for a TV\u2026"
                else
                    "Tap the TV name above to find your Android TV",
                color = RemoteColors.MutedText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        VolumeSlider(
            percent = state.volumePercent,
            onChange = viewModel::setVolume,
            onStep = viewModel::stepVolume
        )

        AppsRow(
            onYoutube = viewModel::launchYoutube,
            onNetflix = viewModel::launchNetflix,
            onPrime = viewModel::launchPrime,
            onSettings = viewModel::openSettings
        )

        Touchpad(
            cursorMode = state.cursorMode,
            onCursorModeToggle = viewModel::toggleCursorMode,
            onDirection = viewModel::dpadDirection,
            onTap = viewModel::dpadTap,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        BottomControls(
            muted = state.muted,
            onVolumeUp = { viewModel.stepVolume(5) },
            onVolumeDown = { viewModel.stepVolume(-5) },
            onBack = viewModel::back,
            onHome = viewModel::home,
            onRecents = viewModel::recentApps,
            onAssistant = viewModel::assistant,
            onMute = viewModel::toggleMute,
            onMic = viewModel::assistant,
            onKeyboard = viewModel::openKeyboard,
            onMore = viewModel::menu,
            modifier = Modifier.height(96.dp)
        )
    }
}
