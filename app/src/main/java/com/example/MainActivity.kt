package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.StudyDatabase
import com.example.data.local.SyncPreferences
import com.example.data.model.StudyTask
import com.example.data.model.SyncStatusUi
import com.example.data.remote.AppsScriptClient
import com.example.data.remote.GoogleAuthHelper
import com.example.data.remote.GoogleSheetsApi
import com.example.data.repository.StudyRepository
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SheetsSyncScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.WeeklyCalendarScreen
import com.example.ui.theme.StudentStudyPlannerTheme
import com.example.ui.viewmodel.StudyPlannerViewModel
import com.example.util.DateHelper

object Routes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_TASKS = "tasks"
    const val ROUTE_WEEKLY = "weekly"
    const val ROUTE_SHEETS = "sheets"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = StudyDatabase.getInstance(applicationContext)
        val syncPreferences = SyncPreferences(applicationContext)
        val googleSheetsApi = GoogleSheetsApi.create()
        val appsScriptClient = AppsScriptClient()
        val repository = StudyRepository(
            taskDao = db.taskDao(),
            syncPreferences = syncPreferences,
            googleSheetsApi = googleSheetsApi,
            appsScriptClient = appsScriptClient
        )
        val authHelper = GoogleAuthHelper(applicationContext)
        val factory = StudyPlannerViewModel.Factory(repository, authHelper)

        setContent {
            StudentStudyPlannerTheme {
                val viewModel: StudyPlannerViewModel = viewModel(factory = factory)
                StudyPlannerApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerApp(viewModel: StudyPlannerViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.ROUTE_DASHBOARD

    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val availableSubjects by viewModel.availableSubjects.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val sheetConfig by viewModel.sheetConfig.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val pendingConsentIntent by viewModel.pendingConsentIntent.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val linkedFocusTask = remember(allTasks, pomodoroState.linkedTaskId) {
        pomodoroState.linkedTaskId?.let { id -> allTasks.firstOrNull { it.id == id } }
    }

    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StudyTask?>(null) }
    var newTaskInitialDeadline by remember { mutableStateOf(DateHelper.todayIso()) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleGoogleAuthActivityResult(
            resultOk = result.resultCode == Activity.RESULT_OK,
            data = result.data
        )
    }

    LaunchedEffect(pendingConsentIntent) {
        pendingConsentIntent?.let { pendingIntent ->
            viewModel.onConsentIntentLaunched()
            consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = Routes.ROUTE_DASHBOARD,
                labelRes = R.string.nav_dashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_dashboard"
            ),
            NavDestinationItem(
                route = Routes.ROUTE_TASKS,
                labelRes = R.string.nav_tasks,
                selectedIcon = Icons.AutoMirrored.Filled.Assignment,
                unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
                testTag = "nav_tasks"
            ),
            NavDestinationItem(
                route = Routes.ROUTE_WEEKLY,
                labelRes = R.string.nav_week,
                selectedIcon = Icons.Filled.CalendarViewWeek,
                unselectedIcon = Icons.Outlined.CalendarViewWeek,
                testTag = "nav_week"
            ),
            NavDestinationItem(
                route = Routes.ROUTE_SHEETS,
                labelRes = R.string.nav_sheets,
                selectedIcon = Icons.Filled.CloudDone,
                unselectedIcon = Icons.Outlined.CloudDone,
                testTag = "nav_sheets"
            )
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        if (sheetConfig.isConfigured) {
                            IconButton(
                                onClick = { viewModel.syncNow(pullOnly = false) },
                                enabled = syncStatus !is SyncStatusUi.Syncing,
                                modifier = Modifier.testTag("topbar_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = stringResource(R.string.cd_sync_now)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != Routes.ROUTE_SHEETS) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingTask = null
                            newTaskInitialDeadline = DateHelper.todayIso()
                            showEditor = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.cd_add_task),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = { Text(stringResource(R.string.action_add_task)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_task")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.ROUTE_DASHBOARD,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    composable(Routes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            summary = dashboardSummary,
                            sheetConfig = sheetConfig,
                            syncStatus = syncStatus,
                            onAddTaskClick = {
                                editingTask = null
                                showEditor = true
                            },
                            onToggleComplete = viewModel::toggleTaskComplete,
                            onCycleStatus = viewModel::cycleTaskStatus,
                            onEditTask = { task ->
                                editingTask = task
                                showEditor = true
                            },
                            onDeleteTask = viewModel::deleteTask,
                            onSyncNow = { viewModel.syncNow(pullOnly = false) },
                            onOpenSheetsTab = { navigateTo(Routes.ROUTE_SHEETS) },
                            onNavigateToTasksWithStatus = { statusFilter ->
                                viewModel.clearFilters()
                                if (statusFilter != null) {
                                    viewModel.selectStatusFilter(statusFilter)
                                }
                                navigateTo(Routes.ROUTE_TASKS)
                            }
                        )
                    }

                    composable(Routes.ROUTE_TASKS) {
                        TasksScreen(
                            tasks = filteredTasks,
                            totalUnfilteredCount = allTasks.size,
                            availableSubjects = availableSubjects,
                            filterState = filterState,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSubjectFilterChange = viewModel::selectSubjectFilter,
                            onStatusFilterChange = viewModel::selectStatusFilter,
                            onPriorityFilterChange = viewModel::selectPriorityFilter,
                            onTypeFilterChange = viewModel::selectTypeFilter,
                            onCycleSortOrder = viewModel::cycleSortOrder,
                            onClearFilters = viewModel::clearFilters,
                            onAddTaskClick = {
                                editingTask = null
                                showEditor = true
                            },
                            onToggleComplete = viewModel::toggleTaskComplete,
                            onCycleStatus = viewModel::cycleTaskStatus,
                            onEditTask = { task ->
                                editingTask = task
                                showEditor = true
                            },
                            onDeleteTask = viewModel::deleteTask,
                            pomodoroState = pomodoroState,
                            linkedFocusTask = linkedFocusTask,
                            onTogglePomodoroRun = viewModel::togglePomodoroRun,
                            onResetPomodoro = viewModel::resetPomodoro,
                            onSelectPomodoroPhase = viewModel::selectPomodoroPhase,
                            onSelectPomodoroFocusMinutes = viewModel::setPomodoroFocusMinutes,
                            onFocusTask = viewModel::linkTaskToPomodoro,
                            onDismissPomodoroBanner = viewModel::dismissPomodoroBanner
                        )
                    }

                    composable(Routes.ROUTE_WEEKLY) {
                        WeeklyCalendarScreen(
                            allTasks = allTasks,
                            onAddTaskForDate = { isoDate ->
                                editingTask = null
                                newTaskInitialDeadline = isoDate
                                showEditor = true
                            },
                            onToggleComplete = viewModel::toggleTaskComplete,
                            onCycleStatus = viewModel::cycleTaskStatus,
                            onEditTask = { task ->
                                editingTask = task
                                showEditor = true
                            },
                            onDeleteTask = viewModel::deleteTask
                        )
                    }

                    composable(Routes.ROUTE_SHEETS) {
                        SheetsSyncScreen(
                            sheetConfig = sheetConfig,
                            syncStatus = syncStatus,
                            taskCount = allTasks.size,
                            onSyncNow = { viewModel.syncNow(pullOnly = false) },
                            onPullOnly = { viewModel.syncNow(pullOnly = true) },
                            onConnectAppsScript = viewModel::connectAppsScript,
                            onConnectExistingGoogleSheet = viewModel::connectExistingGoogleSheet,
                            onCreateNewGoogleSheet = viewModel::createNewGoogleSheet,
                            onAutoSyncChange = viewModel::setAutoSync,
                            onDisconnect = viewModel::disconnectSheet
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorSheet(
            existingTask = editingTask,
            suggestedSubjects = availableSubjects,
            onDismiss = { showEditor = false },
            onSave = { savedTask ->
                viewModel.saveTask(savedTask)
                showEditor = false
            },
            initialDeadline = newTaskInitialDeadline
        )
    }
}
