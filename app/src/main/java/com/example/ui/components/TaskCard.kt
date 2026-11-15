package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyTask
import com.example.data.model.SyncState
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.util.DateHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: StudyTask,
    onToggleComplete: () -> Unit,
    onCycleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isFocusedTask: Boolean = false,
    onFocusTask: (() -> Unit)? = null
) {
    val accentColor by animateColorAsState(
        targetValue = when {
            task.isCompleted -> MaterialTheme.colorScheme.tertiary
            task.isOverdue -> MaterialTheme.colorScheme.error
            task.priority == TaskPriority.HIGH -> MaterialTheme.colorScheme.error
            task.priority == TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "task_accent_color"
    )

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        task.isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        task.isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(if (isSelected || task.isOverdue) 1.5.dp else 1.dp, borderColor),
        tonalElevation = if (task.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("task_toggle_${task.id}")
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = stringResource(R.string.cd_toggle_complete),
                            tint = if (task.isCompleted) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = task.subject,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = task.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onFocusTask != null && !task.isCompleted) {
                            IconButton(
                                onClick = onFocusTask,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = stringResource(R.string.cd_focus_task),
                                    tint = if (isFocusedTask) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.cd_edit_task),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_task),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 48.dp, end = 8.dp)
                    )
                }

                FlowRow(
                    modifier = Modifier.padding(start = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataPill(
                        icon = taskTypeIcon(task.taskType),
                        label = task.taskType.label,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val priorityColors = priorityColors(task.priority)
                    MetadataPill(
                        dotColor = priorityColors.second,
                        label = "${task.priority.label} Priority",
                        containerColor = priorityColors.first,
                        contentColor = priorityColors.second
                    )

                    val statusColors = statusColors(task.status)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCycleStatus)
                            .testTag("status_pill_${task.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = statusColors.first
                    ) {
                        Text(
                            text = task.status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColors.second,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    val deadlineText = DateHelper.formatDeadlineLabel(task.deadline, task.status)
                    val dueSoon = !task.isCompleted && DateHelper.isDueSoon(task.deadline)
                    val deadlineBg = when {
                        task.isOverdue -> MaterialTheme.colorScheme.errorContainer
                        dueSoon -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    }
                    val deadlineFg = when {
                        task.isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                        dueSoon -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    MetadataPill(
                        icon = Icons.Outlined.CalendarToday,
                        label = deadlineText,
                        containerColor = deadlineBg,
                        contentColor = deadlineFg
                    )

                    if (task.syncState == SyncState.PENDING_UPSERT) {
                        Text(
                            text = "• Pending sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    dotColor: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun taskTypeIcon(type: TaskType): ImageVector = when (type) {
    TaskType.ASSIGNMENT -> Icons.AutoMirrored.Outlined.Assignment
    TaskType.PRESENTATION -> Icons.Outlined.PresentToAll
    TaskType.EXAM -> Icons.Outlined.Quiz
    TaskType.STUDY -> Icons.Outlined.MenuBook
    TaskType.OTHER -> Icons.Outlined.TaskAlt
}

@Composable
fun priorityColors(priority: TaskPriority): Pair<Color, Color> = when (priority) {
    TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    TaskPriority.LOW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
fun statusColors(status: TaskStatus): Pair<Color, Color> = when (status) {
    TaskStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}
