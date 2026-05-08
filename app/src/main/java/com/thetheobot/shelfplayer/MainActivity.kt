package com.thetheobot.shelfplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var internalRoute by mutableStateOf(parseInternalAppRoute(null))
    private var internalLaunchEventId by mutableIntStateOf(0)
    private var internalLaunchIntentIsDeepLink by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        internalRoute = parseInternalAppRoute(intent?.data?.encodedPath ?: intent?.data?.path)
        internalLaunchIntentIsDeepLink = intent?.action == Intent.ACTION_VIEW && intent.data != null
        internalLaunchEventId = savedInstanceState?.getInt(KEY_INTERNAL_LAUNCH_EVENT_ID)
            ?: if (internalRoute == null) 0 else 1
        setContent {
            ShelfPlayerApp(
                initialRoute = internalRoute,
                launchEventId = internalLaunchEventId,
                launchIntentIsDeepLink = internalLaunchIntentIsDeepLink,
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        internalRoute = parseInternalAppRoute(intent?.data?.encodedPath ?: intent?.data?.path)
        internalLaunchIntentIsDeepLink = intent?.action == Intent.ACTION_VIEW && intent.data != null
        internalLaunchEventId += 1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_INTERNAL_LAUNCH_EVENT_ID, internalLaunchEventId)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_INTERNAL_LAUNCH_EVENT_ID = "internal_launch_event_id"
    }
}
