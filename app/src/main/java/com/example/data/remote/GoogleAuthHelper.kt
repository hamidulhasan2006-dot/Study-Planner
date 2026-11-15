package com.example.data.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface GoogleAuthOutcome {
    data class Authorized(val accessToken: String) : GoogleAuthOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : GoogleAuthOutcome
    data class Failed(val reason: String) : GoogleAuthOutcome
}

class GoogleAuthHelper(private val context: Context) {
    private val sheetsScope = Scope("https://www.googleapis.com/auth/spreadsheets")

    suspend fun requestSheetsAccess(): GoogleAuthOutcome =
        suspendCancellableCoroutine { continuation ->
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(sheetsScope))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent != null) {
                            continuation.resume(GoogleAuthOutcome.NeedsConsent(pendingIntent))
                        } else {
                            continuation.resume(
                                GoogleAuthOutcome.Failed("Google authorization requires consent, but no prompt was returned.")
                            )
                        }
                    } else {
                        val token = result.accessToken
                        if (!token.isNullOrBlank()) {
                            continuation.resume(GoogleAuthOutcome.Authorized(token))
                        } else {
                            continuation.resume(
                                GoogleAuthOutcome.Failed("No access token returned. Try the 2-minute Apps Script Link option below.")
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(
                        GoogleAuthOutcome.Failed(
                            e.localizedMessage
                                ?: "Google Sign-In unavailable on this device. Use the 2-minute Apps Script Link below."
                        )
                    )
                }
        }

    fun extractTokenFromResultIntent(data: Intent?): String? {
        if (data == null) return null
        return runCatching {
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            result.accessToken
        }.getOrNull()
    }
}
