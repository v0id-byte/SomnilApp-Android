package com.somnil.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.somnil.app.service.BLEMonitoringService
import com.somnil.app.ui.SomnilApp
import com.somnil.app.ui.theme.BackgroundDark
import com.somnil.app.ui.theme.SomnilTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register lifecycle observer for foreground service management
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(this))

        setContent {
            SomnilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    SomnilApp()
                }
            }
        }
    }
}

/**
 * Observes app foreground/background transitions.
 * Starts BLEMonitoringService when app goes to background to keep BLE connection alive.
 */
private class AppLifecycleObserver(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    private var wasInBackground = false

    override fun onStart(owner: LifecycleOwner) {
        if (wasInBackground) {
            // User returned from background - start foreground service to keep BLE alive
            val intent = Intent(activity, BLEMonitoringService::class.java).apply {
                action = BLEMonitoringService.ACTION_START
            }
            activity.startForegroundService(intent)
        }
        wasInBackground = false
    }

    override fun onStop(owner: LifecycleOwner) {
        wasInBackground = true
    }
}