package com.thetheobot.shelfplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShelfPlayerApp(initialRoute = parseInternalAppRoute(intent?.data?.path))
        }
    }
}
