package com.tvremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tvremote.app.ui.RemoteScreen
import com.tvremote.app.ui.theme.TVRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TVRemoteTheme {
                RemoteScreen()
            }
        }
    }
}
