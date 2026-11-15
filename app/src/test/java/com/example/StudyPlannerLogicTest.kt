package com.example

import com.example.data.model.SortOrder
import com.example.data.model.StudyTask
import com.example.data.model.SyncState
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.data.remote.SheetRowParser
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroUiState
import com.example.ui.viewmodel.StudyPlannerViewModel
import com.example.ui.viewmodel.TaskFilterState
import com.example.util.DateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPlannerLogicTest {

    @Test
    fun `sheet row parser parses headers and serializes back`() {
        val raw = listOf(
            listOf("Task ID", "Task Title", "Subject", "Task Type", "Deadline", "Priority", "Status", "Notes"),
            listOf("ST-101", "Calculus Problem Set", "MATH 201", "Assignment", "2026-11-20", "High", "In Progress", "Chapters 4-5"),
            listOf("", "History Slides", "HIST 105", "Presentation", "11/25/2026", "Low", "Completed", "10 slides")
        )

        val parsed = SheetRowParser.parseRows(raw)
        assertEquals(2, parsed.size)
        assertEquals("ST-101", parsed[0].id)
        assertEquals(TaskType.ASSIGNMENT, parsed[0].taskType)
        assertEquals(TaskPriority.HIGH, parsed[0].priority)
        assertEquals(TaskStatus.IN_PROGRESS, parsed[0].status)

        assertTrue(parsed[1].id.startsWith("ST-"))
        assertEquals("2026-11-25", parsed[1].deadline)
        assertEquals(TaskType.PRESENTATION, parsed[1].taskType)
        assertEquals(TaskStatus.COMPLETED, parsed[1].status)
    }

    @Test
    fun `two way merge reconciles local edits deletes and remote additions`() {
        val local = listOf(
            StudyTask(id = "A", title = "Local Updated", subject = "CS 101", syncState = SyncState.PENDING_UPSERT),
            StudyTask(id = "B", title = "To Delete", subject = "CS 101", syncState = SyncState.PENDING_DELETE),
            StudyTask(id = "C", title = "Deleted On Sheet", subject = "CS 101", syncState = SyncState.SYNCED)
        )
        val remote = listOf(
            StudyTask(id = "A", title = "Old Sheet Title", subject = "CS 101", syncState = SyncState.SYNCED),
            StudyTask(id = "B", title = "To Delete", subject = "CS 101", syncState = SyncState.SYNCED),
            StudyTask(id = "D", title = "Added In Sheet", subject = "BIO 110", syncState = SyncState.SYNCED)
        )

        val merged = SheetRowParser.mergeTwoWay(local, remote)
        val byId = merged.associateBy { it.id }

        assertEquals(2, merged.size)
        assertEquals("Local Updated", byId["A"]?.title)
        assertEquals("Added In Sheet", byId["D"]?.title)
        assertFalse(byId.containsKey("B"))
        assertFalse(byId.containsKey("C"))
    }

    @Test
    fun `dashboard summary computes counts and overdue tasks accurately`() {
        val yesterday = DateHelper.addDaysIso(-1)
        val tomorrow = DateHelper.addDaysIso(1)
        val tasks = listOf(
            StudyTask(id = "1", title = "Overdue Essay", subject = "ENG 101", deadline = yesterday, priority = TaskPriority.HIGH, status = TaskStatus.NOT_STARTED),
            StudyTask(id = "2", title = "Lab Quiz", subject = "BIO 102", deadline = tomorrow, priority = TaskPriority.MEDIUM, status = TaskStatus.IN_PROGRESS),
            StudyTask(id = "3", title = "Done Homework", subject = "ENG 101", deadline = yesterday, priority = TaskPriority.LOW, status = TaskStatus.COMPLETED)
        )

        val summary = StudyPlannerViewModel.buildDashboardSummary(tasks)
        assertEquals(3, summary.totalTasks)
        assertEquals(1, summary.completedTasks)
        assertEquals(2, summary.pendingTasks)
        assertEquals(1, summary.overdueTasks)
        assertEquals(2, summary.urgentTasks.size)
        assertEquals("1", summary.urgentTasks.first().id)
    }

    @Test
    fun `filtering and deadline sorting work across all dimensions`() {
        val tasks = listOf(
            StudyTask(id = "1", title = "Midterm Exam", subject = "CS 301", taskType = TaskType.EXAM, deadline = "2026-12-05", priority = TaskPriority.HIGH, status = TaskStatus.NOT_STARTED),
            StudyTask(id = "2", title = "Algorithm Lab", subject = "CS 301", taskType = TaskType.ASSIGNMENT, deadline = "2026-11-20", priority = TaskPriority.MEDIUM, status = TaskStatus.IN_PROGRESS),
            StudyTask(id = "3", title = "Group Slides", subject = "HIST 101", taskType = TaskType.PRESENTATION, deadline = "2026-11-18", priority = TaskPriority.LOW, status = TaskStatus.COMPLETED)
        )

        val filtered = StudyPlannerViewModel.applyFiltersAndSort(
            tasks,
            TaskFilterState(
                searchQuery = "CS 301",
                selectedSubject = "CS 301",
                sortOrder = SortOrder.DEADLINE_SOONEST
            )
        )
        assertEquals(listOf("2", "1"), filtered.map { it.id })
    }

    @Test
    fun `extractSpreadsheetId handles full URL and raw ID`() {
        val url = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0"
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", DateHelper.extractSpreadsheetId(url))
        assertEquals("raw_sheet_id_123", DateHelper.extractSpreadsheetId("raw_sheet_id_123"))
    }

    @Test
    fun `pomodoro formatting and phase transitions advance sessions accurately`() {
        assertEquals("25:00", PomodoroUiState.formatSeconds(25 * 60))
        assertEquals("04:09", PomodoroUiState.formatSeconds(249))
        assertEquals("00:00", PomodoroUiState.formatSeconds(-5))

        val focusDone = StudyPlannerViewModel.onPhaseFinished(
            PomodoroUiState(phase = PomodoroPhase.FOCUS, focusMinutes = 25, completedSessions = 0)
        )
        assertEquals(PomodoroPhase.SHORT_BREAK, focusDone.phase)
        assertEquals(1, focusDone.completedSessions)
        assertEquals(5 * 60, focusDone.remainingSeconds)
        assertFalse(focusDone.isRunning)

        val fourthFocusDone = StudyPlannerViewModel.onPhaseFinished(
            PomodoroUiState(phase = PomodoroPhase.FOCUS, focusMinutes = 25, completedSessions = 3)
        )
        assertEquals(PomodoroPhase.LONG_BREAK, fourthFocusDone.phase)
        assertEquals(4, fourthFocusDone.completedSessions)
        assertEquals(15 * 60, fourthFocusDone.remainingSeconds)
    }

    @Test
    fun `weekly calendar helper generates 7 consecutive days containing reference date`() {
        val days = DateHelper.getWeekDays(weekOffset = 0, referenceTodayIso = "2026-11-15")
        assertEquals(7, days.size)
        assertEquals("2026-11-15", days.first().isoDate)
        assertEquals("2026-11-21", days.last().isoDate)
        assertTrue(days.first().isToday)
        assertEquals(1, days.count { it.isToday })

        val nextWeek = DateHelper.getWeekDays(weekOffset = 1, referenceTodayIso = "2026-11-15")
        assertEquals("2026-11-22", nextWeek.first().isoDate)
        assertEquals("2026-11-28", nextWeek.last().isoDate)
    }
}
