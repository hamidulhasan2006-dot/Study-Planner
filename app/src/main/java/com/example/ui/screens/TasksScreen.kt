package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.ui.components.MetadataPill
import com.example.ui.components.PomodoroTimerCard
import com.example.ui.components.TaskCard
import com.example.ui.components.priorityColors
import com.example.ui.components.statusColors
import com.example.ui.components.taskTypeIcon
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroUiState
import com.example.ui.viewmodel.TaskFilterState
import com.example.util.DateHelper

@Composable
fun TasksScreen(
    tasks: List<StudyTask>,
    totalUnfilteredCount: Int,
    availableSubjects: List<String>,
    filterState: TaskFilterState,
    onSearchQueryChange: (String) -> Unit,
    onSubjectFilterChange: (String?) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onTypeFilterChange: (TaskType?) -> Unit,
    onCycleSortOrder: () -> Unit,
    onClearFilters: () -> Unit,
    onAddTaskClick: () -> Unit,
    onToggleComplete: (StudyTask) -> Unit,
    onCycleStatus: (StudyTask) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    pomodoroState: PomodoroUiState = PomodoroUiState(),
    linkedFocusTask: StudyTask? = null,
    onTogglePomodoroRun: () -> Unit = {},
    onResetPomodoro: () -> Unit = {},
    onSelectPomodoroPhase: (PomodoroPhase) -> Unit = {},
    onSelectPomodoroFocusMinutes: (Int) -> Unit = {},
    onFocusTask: (StudyTask?) -> Unit = {},
    onDismissPomodoroBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var taskPendingDelete by remember { mutableStateOf<StudyTask?>(null) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMedium = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isMedium) 24.dp else 16.dp)
                .padding(top = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.tasks_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${tasks.size} ${if (tasks.size == 1) "task" else "tasks"} shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onCycleSortOrder,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("sort_tasks_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(R.string.cd_sort_deadline),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(filterState.sortOrder.label)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            FilterBarRow(
                availableSubjects = availableSubjects,
                filterState = filterState,
                onSubjectFilterChange = onSubjectFilterChange,
                onStatusFilterChange = onStatusFilterChange,
                onPriorityFilterChange = onPriorityFilterChange,
                onTypeFilterChange = onTypeFilterChange,
                onClearFilters = onClearFilters
            )

            Spacer(modifier = Modifier.height(10.dp))

            PomodoroTimerCard(
                state = pomodoroState,
                linkedTask = linkedFocusTask,
                onToggleRun = onTogglePomodoroRun,
                onReset = onResetPomodoro,
                onSelectPhase = onSelectPomodoroPhase,
                onSelectFocusMinutes = onSelectPomodoroFocusMinutes,
                onClearLinkedTask = { onFocusTask(null) },
                onCompleteLinkedTask = { task ->
                    onToggleComplete(task)
                    onFocusTask(null)
                },
                onDismissBanner = onDismissPomodoroBanner
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (tasks.isEmpty()) {
                EmptyTasksState(
                    isFilteredEmpty = totalUnfilteredCount > 0 && filterState.hasActiveFilters,
                    onAddTaskClick = onAddTaskClick,
                    onClearFilters = onClearFilters,
                    modifier = Modifier.weight(1f)
                )
            } else if (isExpanded) {
                val selectedTask = tasks.firstOrNull { it.id == selectedTaskId } ?: tasks.first()
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                isSelected = task.id == selectedTask.id,
                                isFocusedTask = task.id == pomodoroState.linkedTaskId,
                                onToggleComplete = { onToggleComplete(task) },
                                onCycleStatus = { onCycleStatus(task) },
                                onEdit = {
                                    selectedTaskId = task.id
                                    onEditTask(task)
                                },
                                onDelete = { taskPendingDelete = task },
                                onFocusTask = { onFocusTask(task) }
                            )
                        }
                    }

                    TaskDetailPane(
                        task = selectedTask,
                        onToggleComplete = { onToggleComplete(selectedTask) },
                        onEdit = { onEditTask(selectedTask) },
                        onDelete = { taskPendingDelete = selectedTask },
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isFocusedTask = task.id == pomodoroState.linkedTaskId,
                            onToggleComplete = { onToggleComplete(task) },
                            onCycleStatus = { onCycleStatus(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { taskPendingDelete = task },
                            onFocusTask = { onFocusTask(task) }
                        )
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
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
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
private fun FilterBarRow(
    availableSubjects: List<String>,
    filterState: TaskFilterState,
    onSubjectFilterChange: (String?) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onTypeFilterChange: (TaskType?) -> Unit,
    onClearFilters: () -> Unit
) {
    var showSubjectMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Box {
                FilterChip(
                    selected = filterState.selectedSubject != null,
                    onClick = { showSubjectMenu = true },
                    label = {
                        Text(filterState.selectedSubject ?: stringResource(R.string.filter_all_subjects))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_subject_chip")
                )
                DropdownMenu(
                    expanded = showSubjectMenu,
                    onDismissRequest = { showSubjectMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all_subjects)) },
                        onClick = {
                            onSubjectFilterChange(null)
                            showSubjectMenu = false
                        }
                    )
                    availableSubjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject) },
                            onClick = {
                                onSubjectFilterChange(subject)
                                showSubjectMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Box {
                FilterChip(
                    selected = filterState.selectedStatus != null,
                    onClick = { showStatusMenu = true },
                    label = {
                        Text(filterState.selectedStatus?.label ?: stringResource(R.string.filter_all_statuses))
                    },
                    modifier = Modifier.testTag("filter_status_chip")
                )
                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all_statuses)) },
                        onClick = {
                            onStatusFilterChange(null)
                            showStatusMenu = false
                        }
                    )
                    TaskStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label) },
                            onClick = {
                                onStatusFilterChange(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Box {
                FilterChip(
                    selected = filterState.selectedPriority != null,
                    onClick = { showPriorityMenu = true },
                    label = {
                        Text(filterState.selectedPriority?.label ?: stringResource(R.string.filter_all_priorities))
                    },
                    modifier = Modifier.testTag("filter_priority_chip")
                )
                DropdownMenu(
                    expanded = showPriorityMenu,
                    onDismissRequest = { showPriorityMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all_priorities)) },
                        onClick = {
                            onPriorityFilterChange(null)
                            showPriorityMenu = false
                        }
                    )
                    TaskPriority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority.label) },
                            onClick = {
                                onPriorityFilterChange(priority)
                                showPriorityMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Box {
                FilterChip(
                    selected = filterState.selectedType != null,
                    onClick = { showTypeMenu = true },
                    label = {
                        Text(filterState.selectedType?.label ?: stringResource(R.string.filter_all_types))
                    },
                    modifier = Modifier.testTag("filter_type_chip")
                )
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all_types)) },
                        onClick = {
                            onTypeFilterChange(null)
                            showTypeMenu = false
                        }
                    )
                    TaskType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                onTypeFilterChange(type)
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (filterState.hasActiveFilters) {
            item {
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.testTag("clear_filters_button")
                ) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    isFilteredEmpty: Boolean,
    onAddTaskClick: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_tasks),
            contentDescription = stringResource(R.string.cd_empty_tasks),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 220.dp, height = 165.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFilteredEmpty) {
                stringResource(R.string.empty_filtered_title)
            } else {
                stringResource(R.string.empty_tasks_title)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isFilteredEmpty) {
                stringResource(R.string.empty_filtered_subtitle)
            } else {
                stringResource(R.string.empty_tasks_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (isFilteredEmpty) {
            OutlinedButton(onClick = onClearFilters) {
                Text(stringResource(R.string.clear_filters))
            }
        } else {
            Button(
                onClick = onAddTaskClick,
                modifier = Modifier.testTag("empty_add_task_button")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDetailPane(
    task: StudyTask,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.subject,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = task.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataPill(
                    icon = taskTypeIcon(task.taskType),
                    label = task.taskType.label,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val pColors = priorityColors(task.priority)
                MetadataPill(
                    dotColor = pColors.second,
                    label = "${task.priority.label} Priority",
                    containerColor = pColors.first,
                    contentColor = pColors.second
                )
                val sColors = statusColors(task.status)
                MetadataPill(
                    label = task.status.label,
                    containerColor = sColors.first,
                    contentColor = sColors.second
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Deadline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${DateHelper.formatDeadlineLabel(task.deadline, task.status)} (${task.deadline})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.notes.ifBlank { "No notes added yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleComplete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (task.isCompleted) "Reopen Task" else "Mark Completed")
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_task),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
