package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SheetConfig
import com.example.data.model.SheetConnectionMode
import com.example.data.model.StudyTask
import com.example.data.model.SyncStatusUi
import com.example.data.remote.AppsScriptClient
import com.example.util.DateHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetsSyncScreen(
    sheetConfig: SheetConfig,
    syncStatus: SyncStatusUi,
    taskCount: Int,
    onSyncNow: () -> Unit,
    onPullOnly: () -> Unit,
    onConnectAppsScript: (url: String, tabName: String) -> Unit,
    onConnectExistingGoogleSheet: (idOrUrl: String, tabName: String) -> Unit,
    onCreateNewGoogleSheet: (title: String, tabName: String) -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedMethodIndex by remember {
        mutableIntStateOf(
            if (sheetConfig.mode == SheetConnectionMode.GOOGLE_OAUTH) 1 else 0
        )
    }
    var appsScriptUrl by remember(sheetConfig.appsScriptUrl) {
        mutableStateOf(sheetConfig.appsScriptUrl)
    }
    var sheetUrlOrId by remember(sheetConfig.spreadsheetId) {
        mutableStateOf(sheetConfig.spreadsheetId)
    }
    var tabName by remember(sheetConfig.sheetTabName) {
        mutableStateOf(sheetConfig.sheetTabName.ifBlank { "Tasks" })
    }
    var newSheetTitle by remember { mutableStateOf("Student Study Planner") }
    var copiedBanner by remember { mutableStateOf<String?>(null) }

    val isSyncing = syncStatus is SyncStatusUi.Syncing

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isMedium = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isMedium) 24.dp else 16.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.sheets_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.sheets_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ConnectionStatusCard(
                    sheetConfig = sheetConfig,
                    syncStatus = syncStatus,
                    taskCount = taskCount,
                    isSyncing = isSyncing,
                    onSyncNow = onSyncNow,
                    onPullOnly = onPullOnly,
                    onAutoSyncChange = onAutoSyncChange,
                    onDisconnect = onDisconnect,
                    onOpenInSheets = {
                        if (sheetConfig.spreadsheetId.isNotBlank()) {
                            val uri = Uri.parse(
                                "https://docs.google.com/spreadsheets/d/${sheetConfig.spreadsheetId}/edit"
                            )
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                    }
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Connect Your Google Sheet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedMethodIndex == 0,
                                onClick = { selectedMethodIndex = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("method_apps_script_tab")
                            ) {
                                Text("Easy Sync Link")
                            }
                            SegmentedButton(
                                selected = selectedMethodIndex == 1,
                                onClick = { selectedMethodIndex = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("method_google_account_tab")
                            ) {
                                Text("Google Account")
                            }
                        }

                        if (selectedMethodIndex == 0) {
                            Text(
                                text = "Best for students: connects directly to any personal Google Sheet in 2 minutes using an Apps Script Web App link — no developer setup needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = appsScriptUrl,
                                onValueChange = { appsScriptUrl = it },
                                label = { Text("Apps Script Web App URL") },
                                placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apps_script_url_input")
                            )

                            OutlinedTextField(
                                value = tabName,
                                onValueChange = { tabName = it },
                                label = { Text("Sheet Tab Name") },
                                placeholder = { Text("Tasks") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sheet_tab_input")
                            )

                            Button(
                                onClick = { onConnectAppsScript(appsScriptUrl, tabName) },
                                enabled = !isSyncing && appsScriptUrl.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("connect_apps_script_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddLink,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect & Sync Sheet")
                            }
                        } else {
                            Text(
                                text = "Sign in with your Google Account to create a brand-new Study Planner spreadsheet or link an existing Sheet URL.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = newSheetTitle,
                                onValueChange = { newSheetTitle = it },
                                label = { Text("New Spreadsheet Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_sheet_title_input")
                            )

                            OutlinedTextField(
                                value = tabName,
                                onValueChange = { tabName = it },
                                label = { Text("Sheet Tab Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onCreateNewGoogleSheet(newSheetTitle, tabName) },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_google_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Study Sheet Automatically")
                            }

                            HorizontalDivider()

                            OutlinedTextField(
                                value = sheetUrlOrId,
                                onValueChange = { sheetUrlOrId = it },
                                label = { Text("Or Paste Existing Google Sheet URL / ID") },
                                placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("existing_sheet_url_input")
                            )

                            OutlinedButton(
                                onClick = { onConnectExistingGoogleSheet(sheetUrlOrId, tabName) },
                                enabled = !isSyncing && sheetUrlOrId.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("connect_existing_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TableChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Link Existing Spreadsheet")
                            }
                        }
                    }
                }
            }

            item {
                ColumnStructureCard(
                    copiedBanner = copiedBanner,
                    onCopyHeaders = {
                        val tsv = StudyTask.SHEET_HEADERS.joinToString("\t")
                        clipboard.setText(AnnotatedString(tsv))
                        copiedBanner = "Copied 8 column headers (paste into cell A1 in Google Sheets)"
                    }
                )
            }

            item {
                BeginnerSetupGuideCard(
                    onCopyScript = {
                        clipboard.setText(AnnotatedString(AppsScriptClient.APPS_SCRIPT_CODE))
                        copiedBanner = "Copied Apps Script code! Paste into Extensions > Apps Script."
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    sheetConfig: SheetConfig,
    syncStatus: SyncStatusUi,
    taskCount: Int,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onPullOnly: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onOpenInSheets: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudDone,
                    contentDescription = null,
                    tint = if (sheetConfig.isConfigured) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sheetConfig.isConfigured) {
                            sheetConfig.spreadsheetTitle.ifBlank { "Connected Google Sheet" }
                        } else {
                            "No Google Sheet Connected Yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sheetConfig.isConfigured) {
                            "${sheetConfig.mode.label} • Tab: ${sheetConfig.sheetTabName} • $taskCount tasks"
                        } else {
                            "Tasks are safely saved on your device until you connect a Sheet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            when (syncStatus) {
                is SyncStatusUi.Success -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = syncStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
                is SyncStatusUi.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = syncStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
                else -> Unit
            }

            if (sheetConfig.isConfigured) {
                Text(
                    text = "Last synced: ${DateHelper.formatSyncTimestamp(sheetConfig.lastSyncedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sync_now_button")
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.sheets_sync_now))
                    }

                    OutlinedButton(
                        onClick = onPullOnly,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pull_sheet_button")
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.sheets_pull_only))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-sync task edits",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Push additions, completions, and edits automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = sheetConfig.autoSyncEnabled,
                        onCheckedChange = onAutoSyncChange,
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (sheetConfig.spreadsheetId.isNotBlank()) {
                        TextButton(onClick = onOpenInSheets) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.cd_open_sheet),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Google Sheets")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = onDisconnect,
                        modifier = Modifier.testTag("disconnect_sheet_button")
                    ) {
                        Icon(
                            Icons.Outlined.LinkOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnStructureCard(
    copiedBanner: String?,
    onCopyHeaders: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spreadsheet Columns (A–H)",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onCopyHeaders,
                    modifier = Modifier.testTag("copy_headers_button")
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_headers),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Headers")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudyTask.SHEET_HEADERS.forEachIndexed { index, col ->
                    val letter = ('A' + index)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "$letter: $col",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (!copiedBanner.isNullOrBlank()) {
                Text(
                    text = copiedBanner,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BeginnerSetupGuideCard(onCopyScript: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sheets_connect_guide_title),
                style = MaterialTheme.typography.titleMedium
            )

            GuideStep(
                stepNumber = "1",
                title = "Create a Google Sheet",
                description = "Go to sheets.google.com, create a blank spreadsheet (e.g., \"My Study Planner\"), and name the bottom tab \"Tasks\"."
            )
            GuideStep(
                stepNumber = "2",
                title = "Open Apps Script",
                description = "In your Google Sheet menu, click Extensions → Apps Script. Delete any placeholder code in Code.gs."
            )
            GuideStep(
                stepNumber = "3",
                title = "Paste the Sync Script",
                description = "Tap Copy Apps Script below and paste it into Code.gs, then click the Save icon."
            )

            OutlinedButton(
                onClick = onCopyScript,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copy_apps_script_button")
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.cd_copy_script),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Apps Script Code")
            }

            GuideStep(
                stepNumber = "4",
                title = "Deploy as Web App",
                description = "Click Deploy → New deployment → Select type: Web app. Set Execute as: Me, and Who has access: Anyone. Click Deploy and authorize once."
            )
            GuideStep(
                stepNumber = "5",
                title = "Paste the Web App URL Above",
                description = "Copy the generated https://script.google.com/.../exec URL, paste it into the Easy Sync Link field above, and tap Connect & Sync Sheet."
            )
        }
    }
}

@Composable
private fun GuideStep(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
