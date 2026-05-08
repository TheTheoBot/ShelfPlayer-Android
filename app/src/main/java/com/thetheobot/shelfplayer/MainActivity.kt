package com.thetheobot.shelfplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var launchState by mutableStateOf(AppLaunchState(route = null, eventId = 0, isDeepLink = false))

    private fun internalLaunchRoute(intent: Intent?): AppRoute? {
        return parseInternalAppRoute(intent?.data?.encodedPath ?: intent?.data?.path)
    }

    private fun isDeepLinkIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_VIEW && intent.data != null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchState = appLaunchStateForInitialIntent(
            route = internalLaunchRoute(intent),
            isDeepLink = isDeepLinkIntent(intent),
        )
        setContent {
            ShelfPlayerApp(
                launchState = launchState,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchState = appLaunchStateForNextIntent(
            previousState = launchState,
            route = internalLaunchRoute(intent),
            isDeepLink = isDeepLinkIntent(intent),
        )
    }
}
