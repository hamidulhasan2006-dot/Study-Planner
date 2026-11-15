package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SheetConfig
import com.example.data.model.SyncStatusUi
import com.example.util.DateHelper

@Composable
fun SyncStatusBanner(
    sheetConfig: SheetConfig,
    syncStatus: SyncStatusUi,
    onSyncClick: () -> Unit,
    onOpenSheetsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSyncing = syncStatus is SyncStatusUi.Syncing
    val isError = syncStatus is SyncStatusUi.Error

    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        sheetConfig.isConfigured -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    }

    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        sheetConfig.isConfigured -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val titleText = when {
        isSyncing -> "Synchronizing with Google Sheets…"
        isError -> (syncStatus as SyncStatusUi.Error).message
        syncStatus is SyncStatusUi.Success -> syncStatus.message
        sheetConfig.isConfigured -> {
            val name = sheetConfig.spreadsheetTitle.ifBlank { "Google Sheet" }
            "$name • Synced ${DateHelper.formatSyncTimestamp(sheetConfig.lastSyncedAt)}"
        }
        else -> "Local mode • Tap to connect Google Sheets"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSheetsTab)
            .testTag("sync_status_banner"),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = when {
                        isError -> Icons.Outlined.ErrorOutline
                        sheetConfig.isConfigured -> Icons.Outlined.CloudDone
                        else -> Icons.Outlined.CloudOff
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (sheetConfig.isConfigured) {
                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("banner_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = stringResource(R.string.cd_sync_now),
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
