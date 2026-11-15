package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyTask
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.util.DateHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    existingTask: StudyTask?,
    suggestedSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit,
    initialDeadline: String = DateHelper.todayIso()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existingTask) { mutableStateOf(existingTask?.title.orEmpty()) }
    var subject by remember(existingTask) { mutableStateOf(existingTask?.subject.orEmpty()) }
    var taskType by remember(existingTask) { mutableStateOf(existingTask?.taskType ?: TaskType.ASSIGNMENT) }
    var deadline by remember(existingTask, initialDeadline) {
        mutableStateOf(existingTask?.deadline ?: initialDeadline)
    }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority ?: TaskPriority.MEDIUM) }
    var status by remember(existingTask) { mutableStateOf(existingTask?.status ?: TaskStatus.NOT_STARTED) }
    var notes by remember(existingTask) { mutableStateOf(existingTask?.notes.orEmpty()) }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingTask == null) "New Study Task" else "Edit Study Task",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g., Lab 4 Report or Midterm Review") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Please enter a task title.") }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject / Course") },
                placeholder = { Text("e.g., CS 201 - Data Structures") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_subject_input")
            )

            if (suggestedSubjects.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedSubjects.take(6).forEach { suggestion ->
                        AssistChip(
                            onClick = { subject = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Task Type",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskType.entries.forEach { option ->
                        FilterChip(
                            selected = taskType == option,
                            onClick = { taskType = option },
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = taskTypeIcon(option),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("type_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Deadline",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_deadline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateHelper.formatDeadlineLabel(deadline, TaskStatus.NOT_STARTED),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { deadline = DateHelper.todayIso() },
                        label = { Text("Today") }
                    )
                    AssistChip(
                        onClick = { deadline = DateHelper.addDaysIso(1) },
                        label = { Text("Tomorrow") }
                    )
                    AssistChip(
                        onClick = { deadline = DateHelper.addDaysIso(3) },
                        label = { Text("+3 Days") }
                    )
                    AssistChip(
                        onClick = { deadline = DateHelper.addDaysIso(7) },
                        label = { Text("+1 Week") }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(option.label) },
                            leadingIcon = if (priority == option) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("priority_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label) },
                            leadingIcon = if (status == option) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("status_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Chapters to study, submission link, rubric details…") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_task_button")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                        } else {
                            onSave(
                                (existingTask ?: StudyTask(title = title.trim(), subject = "")).copy(
                                    title = title.trim(),
                                    subject = subject.trim().ifBlank { "General Studies" },
                                    taskType = taskType,
                                    deadline = deadline,
                                    priority = priority,
                                    status = status,
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_task_button")
                ) {
                    Text(stringResource(R.string.action_save_task))
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = DateHelper.isoToUtcMillis(deadline) ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            deadline = DateHelper.epochMillisToIso(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
