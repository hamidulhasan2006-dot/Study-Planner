package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.ui.components.TaskCard
import com.example.ui.components.taskTypeIcon
import com.example.util.DateHelper

@Composable
fun WeeklyCalendarScreen(
    allTasks: List<StudyTask>,
    onAddTaskForDate: (String) -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onCycleStatus: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    val weekDays = remember(weekOffset) { DateHelper.getWeekDays(weekOffset) }
    val todayIso = remember { DateHelper.todayIso() }
    var selectedDateIso by rememberSaveable(weekOffset) {
        mutableStateOf(
            if (weekOffset == 0) todayIso else weekDays.firstOrNull()?.isoDate ?: todayIso
        )
    }
    var showFullWeekAgenda by rememberSaveable { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<StudyTask?>(null) }

    val tasksByDate = remember(allTasks, weekDays) {
        val weekIsoSet = weekDays.map { it.isoDate }.toSet()
        allTasks
            .filter { it.deadline in weekIsoSet }
            .sortedWith(
                compareBy<StudyTask> { it.isCompleted }
                    .thenBy { it.priority.sortRank }
                    .thenBy { it.title }
            )
            .groupBy { it.deadline }
    }

    val weekTasks = remember(tasksByDate, weekDays) {
        weekDays.flatMap { day -> tasksByDate[day.isoDate].orEmpty() }
    }

    val weekCompleted = weekTasks.count { it.isCompleted }
    val weekOverdue = weekTasks.count { it.isOverdue }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedBoard = maxWidth >= 840.dp
        val isMedium = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isMedium) 24.dp else 16.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Week Header + Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.weekly_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = DateHelper.formatWeekRangeLabel(weekDays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { weekOffset -= 1 },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("week_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_previous_week)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            weekOffset = 0
                            selectedDateIso = todayIso
                        },
                        enabled = weekOffset != 0,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.testTag("week_today_button")
                    ) {
                        Text("This Week")
                    }

                    IconButton(
                        onClick = { weekOffset += 1 },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("week_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_week)
                        )
                    }
                }
            }

            // Week Summary Pill Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${weekTasks.size} due this week • $weekCompleted done" +
                            if (weekOverdue > 0) " • $weekOverdue overdue" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (weekOverdue > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (!isExpandedBoard) {
                        TextButton(
                            onClick = { showFullWeekAgenda = !showFullWeekAgenda },
                            modifier = Modifier.testTag("toggle_week_agenda_button")
                        ) {
                            Text(if (showFullWeekAgenda) "By Day" else "Full Week")
                        }
                    }
                }
            }

            if (isExpandedBoard) {
                // Wide screen 7-column weekly planner board
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekDays.forEach { day ->
                        val dayTasks = tasksByDate[day.isoDate].orEmpty()
                        WideDayColumn(
                            day = day,
                            tasks = dayTasks,
                            onAddForDay = { onAddTaskForDate(day.isoDate) },
                            onToggleComplete = onToggleComplete,
                            onEditTask = onEditTask,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                // Mobile / Medium 7-Day Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_day_strip"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekDays.forEach { day ->
                        val dayTasks = tasksByDate[day.isoDate].orEmpty()
                        val isSelected = !showFullWeekAgenda && day.isoDate == selectedDateIso
                        DaySelectorPill(
                            day = day,
                            tasks = dayTasks,
                            isSelected = isSelected,
                            onClick = {
                                showFullWeekAgenda = false
                                selectedDateIso = day.isoDate
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (showFullWeekAgenda) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        weekDays.forEach { day ->
                            val dayTasks = tasksByDate[day.isoDate].orEmpty()
                            item(key = "header_${day.isoDate}") {
                                DayAgendaHeader(
                                    day = day,
                                    taskCount = dayTasks.size,
                                    onAddClick = { onAddTaskForDate(day.isoDate) }
                                )
                            }
                            if (dayTasks.isEmpty()) {
                                item(key = "empty_${day.isoDate}") {
                                    Text(
                                        text = "No deadlines scheduled.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                                    )
                                }
                            } else {
                                items(dayTasks, key = { "${day.isoDate}_${it.id}" }) { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleComplete = { onToggleComplete(task) },
                                        onCycleStatus = { onCycleStatus(task) },
                                        onEdit = { onEditTask(task) },
                                        onDelete = { taskPendingDelete = task }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val selectedDay = weekDays.firstOrNull { it.isoDate == selectedDateIso } ?: weekDays.first()
                    val dayTasks = tasksByDate[selectedDay.isoDate].orEmpty()

                    DayAgendaHeader(
                        day = selectedDay,
                        taskCount = dayTasks.size,
                        onAddClick = { onAddTaskForDate(selectedDay.isoDate) }
                    )

                    if (dayTasks.isEmpty()) {
                        EmptyDayCard(
                            dayLabel = selectedDay.dayFullLabel,
                            onAddClick = { onAddTaskForDate(selectedDay.isoDate) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dayTasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onToggleComplete = { onToggleComplete(task) },
                                    onCycleStatus = { onCycleStatus(task) },
                                    onEdit = { onEditTask(task) },
                                    onDelete = { taskPendingDelete = task }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = {
                Text("\"${task.title}\" — ${stringResource(R.string.delete_dialog_body)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTask(task)
                        taskPendingDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DaySelectorPill(
    day: DateHelper.WeekDayInfo,
    tasks: List<StudyTask>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fgColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .testTag("week_day_pill_${day.isoDate}"),
        shape = MaterialTheme.shapes.small,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = day.dayShortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fgColor
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(8.dp)
            ) {
                tasks.take(3).forEach { task ->
                    val dotColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        task.isCompleted -> MaterialTheme.colorScheme.tertiary
                        task.isOverdue || task.priority == TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayAgendaHeader(
    day: DateHelper.WeekDayInfo,
    taskCount: Int,
    onAddClick: () -> Unit
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
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (day.isToday) "Today • ${day.dayFullLabel}" else day.dayFullLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (taskCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$taskCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        TextButton(
            onClick = onAddClick,
            modifier = Modifier.testTag("add_task_for_day_${day.isoDate}")
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
        }
    }
}

@Composable
private fun EmptyDayCard(
    dayLabel: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No tasks scheduled for $dayLabel",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Schedule a study block, assignment, or presentation deadline for this day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule Task")
            }
        }
    }
}

@Composable
private fun WideDayColumn(
    day: DateHelper.WeekDayInfo,
    tasks: List<StudyTask>,
    onAddForDay: () -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (day.isToday) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (day.isToday) 1.5.dp else 1.dp,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.dayShortLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = day.dayNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onAddForDay,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_add_task),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTask(task) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleComplete(task) },
                                    modifier = Modifier.size(28.dp)
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
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = taskTypeIcon(task.taskType),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = task.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
