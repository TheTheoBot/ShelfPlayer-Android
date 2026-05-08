package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val CONNECT_HINT = "Beispiel: http://books.local oder https://books.example.com"

private enum class ConnectionScreenPhase {
    Idle,
    Editing,
    Validating,
    ValidationError,
    Saving,
    Saved,
}

data class ConnectionFormValidation(
    val serverUrlError: String? = null,
    val accessTokenError: String? = null,
) {
    val isValid: Boolean
        get() = serverUrlError == null && accessTokenError == null
}

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

    val isHttp = uri.scheme?.equals("http", ignoreCase = true) == true
    val isHttps = uri.scheme?.equals("https", ignoreCase = true) == true
    if (!(isHttp || isHttps)) {
        return "Server-URL muss mit http:// oder https:// beginnen"
    }

    val host = uri.host ?: extractHostFromAuthority(trimmed)
    if (host.isNullOrBlank()) {
        return "Server-URL braucht einen Hostnamen oder eine IP-Adresse"
    }

    if (!uri.userInfo.isNullOrBlank()) {
        return "Server-URL darf keine Zugangsdaten enthalten"
    }

    val hasNonRootPath = uri.rawPath != null && uri.rawPath != "/"
    if (hasNonRootPath || uri.rawQuery != null || uri.rawFragment != null) {
        return "Server-URL darf keinen Pfad, keine Query und kein Fragment enthalten"
    }

    return null
}

fun extractHostFromAuthority(rawUrl: String): String? {
    val afterScheme = rawUrl.substringAfter("://", missingDelimiterValue = "")
    if (afterScheme.isBlank()) return null

    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    if (authority.isBlank()) return null

    val withoutUserInfo = authority.substringAfterLast('@')
    if (withoutUserInfo.isBlank()) return null

    return if (withoutUserInfo.startsWith("[")) {
        withoutUserInfo.substringBefore(']').removePrefix("[").takeIf { it.isNotBlank() }
    } else {
        withoutUserInfo.substringBefore(':').takeIf { it.isNotBlank() }
    }
}

fun serverUrlSecurityWarning(raw: String): String? {
    val uri = runCatching { java.net.URI(raw.trim()) }.getOrNull() ?: return null
    val isHttp = uri.scheme?.equals("http", ignoreCase = true) == true
    return if (isHttp) {
        "Warnung: HTTP ist unverschlüsselt. Für lokale Setups okay, im Internet bitte HTTPS verwenden."
    } else {
        null
    }
}

fun validateAccessToken(raw: String): String? {
    return if (raw.isBlank()) "Access Token fehlt" else null
}

fun validateConnectionForm(serverUrl: String, accessToken: String): ConnectionFormValidation {
    return ConnectionFormValidation(
        serverUrlError = validateServerUrl(serverUrl),
        accessTokenError = validateAccessToken(accessToken),
    )
}

fun hasConnectionInputs(serverUrl: String, accessToken: String): Boolean {
    return serverUrl.trim().isNotBlank() && accessToken.trim().isNotBlank()
}

@Composable
fun ConnectionScreen(
    padding: PaddingValues,
    connectionSession: ConnectionSession,
    onConnectionSaved: suspend (ConnectionCredentials) -> Boolean,
    onConnectionTested: suspend (ConnectionCredentials) -> ConnectionVerificationResult = ::verifyConnection,
) {
    var serverUrl by rememberSaveable { mutableStateOf(connectionSession.serverUrl) }
    var accessToken by remember { mutableStateOf("") }
    var screenPhase by rememberSaveable { mutableStateOf(ConnectionScreenPhase.Idle) }
    var bannerMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var bannerIsError by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionSession.serverUrl) {
        if (serverUrl.isBlank()) {
            serverUrl = connectionSession.serverUrl
        }
    }

    val validation = if (screenPhase == ConnectionScreenPhase.ValidationError || screenPhase == ConnectionScreenPhase.Saving || screenPhase == ConnectionScreenPhase.Validating) {
        validateConnectionForm(serverUrl, accessToken)
    } else {
        ConnectionFormValidation()
    }
    val canSubmit = hasConnectionInputs(serverUrl, accessToken) && screenPhase != ConnectionScreenPhase.Validating && screenPhase != ConnectionScreenPhase.Saving

    fun markEditing() {
        screenPhase = ConnectionScreenPhase.Editing
        bannerMessage = null
        bannerIsError = false
    }

    fun showError(message: String) {
        screenPhase = ConnectionScreenPhase.ValidationError
        bannerMessage = message
        bannerIsError = true
    }

    fun showSuccess(message: String, phase: ConnectionScreenPhase = ConnectionScreenPhase.Editing) {
        screenPhase = phase
        bannerMessage = message
        bannerIsError = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Audiobookshelf verbinden")
        Text(
            "Gib Server-URL und Access Token ein; die App prüft die Eingaben lokal, kann die Verbindung per HTTP testen und speichert die Verbindung verschlüsselt.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )

        if (bannerMessage != null) {
            val currentBannerMessage = bannerMessage
            Card(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (bannerIsError) {
                        androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (bannerIsError) {
                        androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(if (bannerIsError) "Verbindung konnte nicht geprüft werden" else "Verbindung geprüft")
                    Text(currentBannerMessage ?: "")
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = serverUrl,
            onValueChange = { newValue ->
                serverUrl = newValue
                markEditing()
            },
            label = { Text("Server-URL") },
            placeholder = { Text(CONNECT_HINT) },
            singleLine = true,
            isError = validation.serverUrlError != null,
        )
        if (validation.serverUrlError != null) {
            Text(validation.serverUrlError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        } else {
            val warningMessage = serverUrlSecurityWarning(serverUrl)
            if (warningMessage != null) {
                Text(
                    warningMessage,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = accessToken,
            onValueChange = { newValue ->
                accessToken = newValue
                markEditing()
            },
            label = { Text("Access Token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            isError = validation.accessTokenError != null,
        )
        if (validation.accessTokenError != null) {
            Text(validation.accessTokenError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        screenPhase = ConnectionScreenPhase.Validating
                        val currentValidation = validateConnectionForm(serverUrl, accessToken)
                        if (!currentValidation.isValid) {
                            showError("Bitte korrigiere die markierten Eingaben.")
                            return@launch
                        }

                        val result = runSuspendCatchingPreservingCancellation {
                            onConnectionTested(
                                ConnectionCredentials(
                                    serverUrl = normalizeServerUrl(serverUrl),
                                    accessToken = accessToken,
                                ),
                            )
                        }.getOrElse { throwable ->
                            showError("Verbindungstest fehlgeschlagen: ${throwable.message ?: "unbekannter Fehler"}")
                            return@launch
                        }

                        when (result) {
                            ConnectionVerificationResult.Success -> {
                                showSuccess("Verbindung erfolgreich geprüft")
                            }

                            is ConnectionVerificationResult.Failure -> {
                                showError(result.message)
                            }
                        }
                    }
                },
                enabled = canSubmit,
            ) {
                Text("Verbindung testen")
            }

            Button(
                onClick = {
                    scope.launch {
                        screenPhase = ConnectionScreenPhase.Saving
                        val currentValidation = validateConnectionForm(serverUrl, accessToken)
                        if (!currentValidation.isValid) {
                            showError("Bitte korrigiere die markierten Eingaben.")
                            return@launch
                        }

                        val normalizedServerUrl = normalizeServerUrl(serverUrl)
                        val success = runSuspendCatchingPreservingCancellation {
                            onConnectionSaved(
                                ConnectionCredentials(
                                    serverUrl = normalizedServerUrl,
                                    accessToken = accessToken,
                                )
                            )
                        }.getOrElse { throwable ->
                            showError("Verbindung konnte nicht gespeichert werden: ${throwable.message ?: "unbekannter Fehler"}")
                            return@launch
                        }
                        if (success) {
                            accessToken = ""
                            serverUrl = normalizedServerUrl
                            showSuccess("Verbindung gespeichert", phase = ConnectionScreenPhase.Saved)
                        } else {
                            showError("Verbindung konnte nicht gespeichert werden")
                        }
                    }
                },
                enabled = canSubmit,
            ) {
                Text(if (screenPhase == ConnectionScreenPhase.Saving) "Speichere..." else "Speichern & weiter")
            }
        }

        if (screenPhase == ConnectionScreenPhase.Validating) {
            Text("Verbindung wird geprüft…")
        } else if (screenPhase == ConnectionScreenPhase.Saving) {
            Text("Verbindung wird gespeichert…")
        }

        if (screenPhase == ConnectionScreenPhase.Saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.elevatedCardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verbindung gespeichert")
                    Text("Server: ${normalizeServerUrl(serverUrl)}")
                    Text("Server-URL und Token werden verschlüsselt auf dem Gerät gespeichert.")
                }
            }
        } else if (screenPhase == ConnectionScreenPhase.Editing || screenPhase == ConnectionScreenPhase.Idle) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.elevatedCardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bereit zum Testen")
                    Text("Die Eingaben sind noch nicht gespeichert.")
                }
            }
        }
    }
}
