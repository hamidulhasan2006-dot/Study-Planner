package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DashboardSummary
import com.example.data.model.SheetConfig
import com.example.data.model.StudyTask
import com.example.data.model.SubjectProgress
import com.example.data.model.SyncStatusUi
import com.example.data.model.TaskStatus
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.TaskCard

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    sheetConfig: SheetConfig,
    syncStatus: SyncStatusUi,
    onAddTaskClick: () -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onCycleStatus: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onSyncNow: () -> Unit,
    onOpenSheetsTab: () -> Unit,
    onNavigateToTasksWithStatus: (TaskStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
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
                HeroAcademicBanner(
                    totalTasks = summary.totalTasks,
                    overdueCount = summary.overdueTasks,
                    onAddTaskClick = onAddTaskClick
                )
            }

            item {
                SyncStatusBanner(
                    sheetConfig = sheetConfig,
                    syncStatus = syncStatus,
                    onSyncClick = onSyncNow,
                    onOpenSheetsTab = onOpenSheetsTab
                )
            }

            item {
                MetricsGrid(
                    summary = summary,
                    isWide = isMedium,
                    onMetricClick = onNavigateToTasksWithStatus
                )
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProgressOverviewCard(summary = summary)
                            if (summary.subjectBreakdown.isNotEmpty()) {
                                SubjectBreakdownCard(subjects = summary.subjectBreakdown)
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1.25f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UrgentSectionHeader(
                                urgentCount = summary.urgentTasks.size,
                                onViewAllTasks = { onNavigateToTasksWithStatus(null) }
                            )
                            if (summary.urgentTasks.isEmpty()) {
                                EmptyUrgentCard(
                                    hasAnyTasks = summary.totalTasks > 0,
                                    onAddTaskClick = onAddTaskClick
                                )
                            } else {
                                summary.urgentTasks.forEach { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleComplete = { onToggleComplete(task) },
                                        onCycleStatus = { onCycleStatus(task) },
                                        onEdit = { onEditTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    ProgressOverviewCard(summary = summary)
                }

                item {
                    UrgentSectionHeader(
                        urgentCount = summary.urgentTasks.size,
                        onViewAllTasks = { onNavigateToTasksWithStatus(null) }
                    )
                }

                if (summary.urgentTasks.isEmpty()) {
                    item {
                        EmptyUrgentCard(
                            hasAnyTasks = summary.totalTasks > 0,
                            onAddTaskClick = onAddTaskClick
                        )
                    }
                } else {
                    items(summary.urgentTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onToggleComplete(task) },
                            onCycleStatus = { onCycleStatus(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }

                if (summary.subjectBreakdown.isNotEmpty()) {
                    item {
                        SubjectBreakdownCard(subjects = summary.subjectBreakdown)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun HeroAcademicBanner(
    totalTasks: Int,
    overdueCount: Int,
    onAddTaskClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_campus_study_banner),
                contentDescription = stringResource(R.string.cd_app_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            overdueCount > 0 -> "$overdueCount overdue ${if (overdueCount == 1) "task needs" else "tasks need"} attention today."
                            totalTasks == 0 -> stringResource(R.string.dashboard_subtitle)
                            else -> "All deadlines are on track. Keep up the momentum!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdueCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onAddTaskClick,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("dashboard_add_task_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_add_task))
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(
    summary: DashboardSummary,
    isWide: Boolean,
    onMetricClick: (TaskStatus?) -> Unit
) {
    val cards = listOf(
        MetricCardData(
            label = stringResource(R.string.metric_total),
            value = summary.totalTasks,
            icon = Icons.AutoMirrored.Outlined.ListAlt,
            container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            testTag = "metric_total_card",
            filterStatus = null
        ),
        MetricCardData(
            label = stringResource(R.string.metric_completed),
            value = summary.completedTasks,
            icon = Icons.Outlined.CheckCircleOutline,
            container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            testTag = "metric_completed_card",
            filterStatus = TaskStatus.COMPLETED
        ),
        MetricCardData(
            label = stringResource(R.string.metric_pending),
            value = summary.pendingTasks,
            icon = Icons.Outlined.PendingActions,
            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = "metric_pending_card",
            filterStatus = TaskStatus.IN_PROGRESS
        ),
        MetricCardData(
            label = stringResource(R.string.metric_overdue),
            value = summary.overdueTasks,
            icon = Icons.Outlined.ErrorOutline,
            container = if (summary.overdueTasks > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            content = if (summary.overdueTasks > 0) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            testTag = "metric_overdue_card",
            filterStatus = TaskStatus.NOT_STARTED
        )
    )

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.forEach { data ->
                MetricCard(
                    data = data,
                    onClick = { onMetricClick(data.filterStatus) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    data = cards[0],
                    onClick = { onMetricClick(cards[0].filterStatus) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    data = cards[1],
                    onClick = { onMetricClick(cards[1].filterStatus) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    data = cards[2],
                    onClick = { onMetricClick(cards[2].filterStatus) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    data = cards[3],
                    onClick = { onMetricClick(cards[3].filterStatus) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class MetricCardData(
    val label: String,
    val value: Int,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    val testTag: String,
    val filterStatus: TaskStatus?
)

@Composable
private fun MetricCard(
    data: MetricCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag(data.testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = data.container,
        border = BorderStroke(1.dp, data.content.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = data.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.content,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = data.value.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = data.content
            )
        }
    }
}

@Composable
private fun ProgressOverviewCard(summary: DashboardSummary) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.completionFraction,
        animationSpec = tween(durationMillis = 550),
        label = "study_progress_ring"
    )
    val percent = (animatedProgress * 100).toInt()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_overview_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 9.dp.toPx()
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
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_overview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${summary.completedTasks} of ${summary.totalTasks} tasks completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusDotLabel(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "To Do: ${summary.notStartedTasks}"
                    )
                    StatusDotLabel(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Active: ${summary.inProgressTasks}"
                    )
                    StatusDotLabel(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Done: ${summary.completedTasks}"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDotLabel(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UrgentSectionHeader(
    urgentCount: Int,
    onViewAllTasks: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${stringResource(R.string.urgent_section_title)} ($urgentCount)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TextButton(onClick = onViewAllTasks) {
            Text("View All")
        }
    }
}

@Composable
private fun EmptyUrgentCard(
    hasAnyTasks: Boolean,
    onAddTaskClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasAnyTasks) {
                        stringResource(R.string.empty_urgent_message)
                    } else {
                        "No study tasks yet"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hasAnyTasks) {
                        "High-priority tasks and deadlines within 3 days appear here automatically."
                    } else {
                        "Tap New Task to add assignments, presentations, or exam prep."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!hasAnyTasks) {
                TextButton(onClick = onAddTaskClick) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun SubjectBreakdownCard(subjects: List<SubjectProgress>) {
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
                text = stringResource(R.string.subject_breakdown_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            subjects.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${item.completed}/${item.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
