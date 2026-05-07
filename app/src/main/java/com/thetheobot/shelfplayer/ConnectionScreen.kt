package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private const val CONNECT_HINT = "Beispiel: https://books.example.com"

fun normalizeServerUrl(raw: String): String = raw.trim().trimEnd('/')

fun validateServerUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "Server-URL fehlt"

    val schemeSeparatorIndex = trimmed.indexOf("://")
    if (schemeSeparatorIndex >= 0) {
        val authorityRemainder = trimmed.substring(schemeSeparatorIndex + 3)
        if (authorityRemainder.isBlank() || authorityRemainder.all { it == '/' }) {
            return "Server-URL braucht einen Hostnamen"
        }
    }

    val uri = runCatching { java.net.URI(trimmed) }.getOrNull()
        ?: return "Server-URL ist ungültig"

    if (!(uri.scheme?.equals("http", ignoreCase = true) == true || uri.scheme?.equals("https", ignoreCase = true) == true)) {
        return "Server-URL muss mit http:// oder https:// beginnen"
    }

    if (uri.host.isNullOrBlank()) {
        return "Server-URL braucht einen Hostnamen"
    }

    if (uri.scheme?.equals("http", ignoreCase = true) == true && !isLocalDevelopmentHost(uri.host)) {
        return "Server-URL muss mit https:// beginnen"
    }

    return null
}

private fun isLocalDevelopmentHost(host: String?): Boolean {
    return when (host?.lowercase()) {
        "localhost",
        "127.0.0.1",
        "::1",
        "10.0.2.2" -> true
        else -> false
    }
}

fun validateAccessToken(raw: String): String? {
    return if (raw.isBlank()) "Access Token fehlt" else null
}

@Composable
fun ConnectionScreen(padding: PaddingValues) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var accessToken by rememberSaveable { mutableStateOf("") }
    var savedServerUrl by rememberSaveable { mutableStateOf("") }
    var connectionSaved by rememberSaveable { mutableStateOf(false) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val serverUrlError = if (attemptedSave) validateServerUrl(serverUrl) else null
    val accessTokenError = if (attemptedSave) validateAccessToken(accessToken) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Audiobookshelf verbinden")
        Text(
            "Gib Server-URL und Access Token ein; die Verbindung wird für diesen App-Lauf vorgemerkt.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                attemptedSave = false
                connectionSaved = false
            },
            label = { Text("Server-URL") },
            placeholder = { Text(CONNECT_HINT) },
            singleLine = true,
            isError = serverUrlError != null,
        )
        if (serverUrlError != null) {
            Text(serverUrlError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = accessToken,
            onValueChange = {
                accessToken = it
                attemptedSave = false
                connectionSaved = false
            },
            label = { Text("Access Token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            isError = accessTokenError != null,
        )
        if (accessTokenError != null) {
            Text(accessTokenError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                attemptedSave = true
                val urlError = validateServerUrl(serverUrl)
                val tokenError = validateAccessToken(accessToken)

                if (urlError == null && tokenError == null) {
                    savedServerUrl = normalizeServerUrl(serverUrl)
                    connectionSaved = true
                    attemptedSave = false
                }
            },
            enabled = serverUrl.isNotBlank() && accessToken.isNotBlank(),
        ) {
            Text("Verbindung speichern")
        }

        if (connectionSaved) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.elevatedCardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verbindung vorgemerkt")
                    Text("Server: $savedServerUrl")
                    Text("Access Token bleibt nur im Arbeitsspeicher dieses App-Laufs.")
                }
            }
        }
    }
}
