package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StudyTask
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroTimerCard(
    state: PomodoroUiState,
    linkedTask: StudyTask?,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSelectFocusMinutes: (Int) -> Unit,
    onClearLinkedTask: () -> Unit,
    onCompleteLinkedTask: (StudyTask) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val phaseColor = when (state.phase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
    }

    val phaseContainerColor = when (state.phase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    }

    val progressFraction = if (state.totalSeconds > 0) {
        state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "pomodoro_ring_progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_timer_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (state.isRunning) 1.5.dp else 1.dp,
            color = if (state.isRunning) phaseColor else MaterialTheme.colorScheme.outline
        ),
        tonalElevation = if (state.isRunning) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact always-accessible control bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = phaseContainerColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = phaseColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = state.phase.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = phaseColor
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.formattedRemaining,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("pomodoro_time_readout")
                    )
                    val subtitle = linkedTask?.title
                        ?: if (state.completedSessions > 0) {
                            "${state.completedSessions} focus ${if (state.completedSessions == 1) "session" else "sessions"} done"
                        } else {
                            stringResource(R.string.pomodoro_subtitle)
                        }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (linkedTask != null) phaseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleRun,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pomodoro_start_pause_button")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isRunning) R.string.cd_pomodoro_pause else R.string.cd_pomodoro_start
                        ),
                        tint = phaseColor
                    )
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pomodoro_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = stringResource(R.string.cd_pomodoro_reset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pomodoro_expand_toggle")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(R.string.cd_pomodoro_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.completionMessage != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.completionMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(220))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Phase selector chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PomodoroPhase.entries.forEach { phase ->
                            FilterChip(
                                selected = state.phase == phase,
                                onClick = { onSelectPhase(phase) },
                                label = { Text(phase.label) },
                                modifier = Modifier.testTag("pomodoro_phase_${phase.name.lowercase()}")
                            )
                        }
                    }

                    // Circular focus timer ring
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = phaseColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.formattedRemaining,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.isRunning) "Running" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = phaseColor
                            )
                        }
                    }

                    // Focus duration presets
                    if (state.phase == PomodoroPhase.FOCUS) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(15, 25, 50).forEach { minutes ->
                                FilterChip(
                                    selected = state.focusMinutes == minutes,
                                    onClick = { onSelectFocusMinutes(minutes) },
                                    label = { Text("${minutes}m") },
                                    modifier = Modifier.testTag("pomodoro_preset_${minutes}m")
                                )
                            }
                        }
                    }

                    // Primary Start / Pause + Reset controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleRun,
                            colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pomodoro_expanded_start_pause")
                        ) {
                            Icon(
                                imageVector = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isRunning) "Pause" else "Start Session")
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pomodoro_expanded_reset")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }

                    // Linked Task Focus Strip
                    if (linkedTask != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = phaseContainerColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Focused Task • ${linkedTask.subject}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = phaseColor
                                    )
                                    Text(
                                        text = linkedTask.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!linkedTask.isCompleted) {
                                    TextButton(
                                        onClick = { onCompleteLinkedTask(linkedTask) },
                                        modifier = Modifier.testTag("pomodoro_complete_linked_task")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Done")
                                    }
                                }
                                IconButton(
                                    onClick = onClearLinkedTask,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.cd_clear_focus_task),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
