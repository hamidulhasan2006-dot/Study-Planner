package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SheetConfig
import com.example.data.model.SheetConnectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(name = "sheet_sync_prefs")

class SyncPreferences(private val context: Context) {
    private object Keys {
        val MODE = stringPreferencesKey("connection_mode")
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SPREADSHEET_TITLE = stringPreferencesKey("spreadsheet_title")
        val SHEET_TAB_NAME = stringPreferencesKey("sheet_tab_name")
        val APPS_SCRIPT_URL = stringPreferencesKey("apps_script_url")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    }

    val sheetConfigFlow: Flow<SheetConfig> = context.syncDataStore.data.map { prefs ->
        val mode = runCatching {
            SheetConnectionMode.valueOf(prefs[Keys.MODE] ?: SheetConnectionMode.NONE.name)
        }.getOrDefault(SheetConnectionMode.NONE)
        SheetConfig(
            mode = mode,
            spreadsheetId = prefs[Keys.SPREADSHEET_ID].orEmpty(),
            spreadsheetTitle = prefs[Keys.SPREADSHEET_TITLE].orEmpty(),
            sheetTabName = prefs[Keys.SHEET_TAB_NAME]?.takeIf { it.isNotBlank() } ?: "Tasks",
            appsScriptUrl = prefs[Keys.APPS_SCRIPT_URL].orEmpty(),
            autoSyncEnabled = prefs[Keys.AUTO_SYNC] ?: true,
            lastSyncedAt = prefs[Keys.LAST_SYNCED_AT] ?: 0L,
            connectedAccountEmail = prefs[Keys.ACCOUNT_EMAIL].orEmpty()
        )
    }

    suspend fun saveGoogleOAuthSheet(
        spreadsheetId: String,
        spreadsheetTitle: String,
        tabName: String,
        accountEmail: String = ""
    ) {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.MODE] = SheetConnectionMode.GOOGLE_OAUTH.name
            prefs[Keys.SPREADSHEET_ID] = spreadsheetId.trim()
            prefs[Keys.SPREADSHEET_TITLE] = spreadsheetTitle.trim()
            prefs[Keys.SHEET_TAB_NAME] = tabName.trim().ifBlank { "Tasks" }
            if (accountEmail.isNotBlank()) {
                prefs[Keys.ACCOUNT_EMAIL] = accountEmail.trim()
            }
        }
    }

    suspend fun saveAppsScriptUrl(url: String, tabName: String = "Tasks") {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.MODE] = SheetConnectionMode.APPS_SCRIPT.name
            prefs[Keys.APPS_SCRIPT_URL] = url.trim()
            prefs[Keys.SHEET_TAB_NAME] = tabName.trim().ifBlank { "Tasks" }
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC] = enabled
        }
    }

    suspend fun updateLastSynced(timestamp: Long, spreadsheetTitle: String? = null) {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_AT] = timestamp
            if (!spreadsheetTitle.isNullOrBlank()) {
                prefs[Keys.SPREADSHEET_TITLE] = spreadsheetTitle
            }
        }
    }

    suspend fun disconnectSheet() {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.MODE] = SheetConnectionMode.NONE.name
            prefs[Keys.LAST_SYNCED_AT] = 0L
        }
    }
}
