package org.distributed.calendar.android

import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.distributed.calendar.android.ui.MainScreen
import org.distributed.calendar.android.service.SyncForegroundService
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        startService(
            Intent(
                this,
                SyncForegroundService::class.java
            )
        )

        setContent {
            MainScreen()
        }
    }
}
