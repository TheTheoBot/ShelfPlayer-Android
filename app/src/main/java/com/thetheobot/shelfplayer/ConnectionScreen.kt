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
    val normalized = normalizeServerUrl(raw)
    if (normalized.isBlank()) return "Server-URL fehlt"

    val uri = runCatching { java.net.URI(normalized) }.getOrNull()
        ?: return "Server-URL ist ungültig"

    if (uri.scheme != "http" && uri.scheme != "https") {
        return "Server-URL muss mit http:// oder https:// beginnen"
    }

    if (uri.host.isNullOrBlank()) {
        return "Server-URL braucht einen Hostnamen"
    }

    return null
}

fun validateAccessToken(raw: String): String? {
    return if (raw.isBlank()) "Access Token fehlt" else null
}

@Composable
fun ConnectionScreen(padding: PaddingValues) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }
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
                val normalizedUrl = normalizeServerUrl(serverUrl)
                val urlError = validateServerUrl(normalizedUrl)
                val tokenError = validateAccessToken(accessToken)

                if (urlError == null && tokenError == null) {
                    savedServerUrl = normalizedUrl
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
