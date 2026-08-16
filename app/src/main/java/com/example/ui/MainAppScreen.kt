package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.SyncState
import com.example.ui.components.AboutDeveloperDialog
import com.example.ui.components.ExitConfirmDialog
import com.example.ui.components.FirebaseCloudDialog
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddReadingScreen
import com.example.ui.screens.BillCycleScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MetersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSplashScreen by rememberSaveable { mutableStateOf(true) }
    var showAuthScreen by rememberSaveable { mutableStateOf(!viewModel.authManager.isUserSignedIn()) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    LaunchedEffect(currentUser) {
        if (currentUser == null && !viewModel.authManager.isUserSignedIn()) {
            showAuthScreen = true
        }
    }
    val selectedScreen by viewModel.selectedNavigationScreen.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(selectedScreen) }
    LaunchedEffect(selectedScreen) {
        if (currentScreen != selectedScreen) {
            currentScreen = selectedScreen
        }
    }
    val dashboardOverview by viewModel.dashboardOverview.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val autoSyncCountdown by viewModel.autoSyncCountdown.collectAsStateWithLifecycle()
    val autoSyncActivityName by viewModel.autoSyncActivityName.collectAsStateWithLifecycle()

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // App Exit BackHandler
    BackHandler(enabled = true) {
        if (showAuthScreen) {
            viewModel.openExitConfirmDialog()
        } else if (currentScreen != Screen.DASHBOARD) {
            currentScreen = Screen.DASHBOARD
        } else {
            viewModel.openExitConfirmDialog()
        }
    }

    // Dialog states
    val showCloudDialog by viewModel.showCloudDialog.collectAsStateWithLifecycle()
    val showDeveloperDialog by viewModel.showDeveloperDialog.collectAsStateWithLifecycle()
    val showThemeDialog by viewModel.showThemeDialog.collectAsStateWithLifecycle()
    val showExitConfirmDialog by viewModel.showExitConfirmDialog.collectAsStateWithLifecycle()

    if (showExitConfirmDialog) {
        ExitConfirmDialog(
            onConfirmExit = {
                viewModel.dismissExitConfirmDialog()
                (context as? Activity)?.finish()
            },
            onDismiss = { viewModel.dismissExitConfirmDialog() }
        )
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissThemeDialog() }
        )
    }

    if (showCloudDialog) {
        FirebaseCloudDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissCloudDialog() }
        )
    }

    if (showDeveloperDialog) {
        AboutDeveloperDialog(
            onDismiss = { viewModel.dismissDeveloperDialog() }
        )
    }

    var showProfileDialog by remember { mutableStateOf(false) }

    // 1. Show Splash Screen on initial launch
    if (showSplashScreen) {
        SplashScreen(
            onSplashFinished = {
                showSplashScreen = false
                if (!viewModel.authManager.isUserSignedInWithGoogle()) {
                    viewModel.authManager.setGuestMode(context)
                    android.widget.Toast.makeText(context, "Continue as Guest", android.widget.Toast.LENGTH_SHORT).show()
                    showAuthScreen = false
                } else {
                    showAuthScreen = false
                }
            }
        )
        return
    }

    // 2. Show Google Sign-In Screen if explicitly requested
    if (showAuthScreen) {
        com.example.ui.screens.GoogleSignInScreen(
            viewModel = viewModel,
            onContinue = { showAuthScreen = false },
            onBack = { showAuthScreen = false }
        )
        return
    }
    
    if (showProfileDialog) {
        com.example.ui.components.ProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false },
            onSignOut = { 
                viewModel.signOut(context)
                showProfileDialog = false
                showAuthScreen = true
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚡ ${currentScreen.title}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                    
                    // Account / Sign-In button
                    IconButton(
                        onClick = { 
                            if (viewModel.authManager.isUserSignedInWithGoogle()) {
                                showProfileDialog = true
                            } else {
                                showAuthScreen = true
                            }
                        },
                        modifier = Modifier.testTag("top_bar_account_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser != null && currentUser?.isAnonymous == false) {
                                Text(
                                    text = currentUser?.displayName?.firstOrNull()?.uppercase() ?: "G",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Sign In",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Theme / Palette / Dark Mode selector button
                    IconButton(
                        onClick = { viewModel.openThemeDialog() },
                        modifier = Modifier.testTag("top_bar_theme_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes & Appearance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Developer info icon button
                    IconButton(
                        onClick = { viewModel.openDeveloperDialog() },
                        modifier = Modifier.testTag("top_bar_developer_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "About Developer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Firebase Cloud Sync button
                    IconButton(
                        onClick = { viewModel.openCloudDialog() },
                        modifier = Modifier.testTag("top_bar_cloud_button")
                    ) {
                        val (icon, color) = when (syncStatus.state) {
                            SyncState.SYNCED -> Pair(Icons.Default.CloudDone, SuccessGreen)
                            SyncState.SYNCING -> Pair(Icons.Default.CloudSync, Color(0xFFD97706))
                            SyncState.OFFLINE -> Pair(Icons.Default.CloudOff, Color(0xFFDC2626))
                            SyncState.ERROR -> Pair(Icons.Default.SyncProblem, Color(0xFFEA580C))
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Firebase Cloud Firestore",
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            viewModel.selectedNavigationScreen.value = screen
                        },
                        icon = {
                            if (screen == Screen.DASHBOARD && dashboardOverview.totalAlertsCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AlertRed) {
                                            Text(
                                                text = "${dashboardOverview.totalAlertsCount}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Automatic 10-second sync countdown indicator
            AnimatedVisibility(
                visible = autoSyncCountdown != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val count = autoSyncCountdown ?: 10
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (count.toFloat() / 10f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-syncing in ${count}s",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (autoSyncActivityName.isNotBlank()) "Uploading to Firebase: $autoSyncActivityName" else "Uploading & updating records in Firebase...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Syncing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { targetScreen ->
                    when (targetScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { screen -> currentScreen = screen }
                        )
                        Screen.ADD_READING -> AddReadingScreen(
                            viewModel = viewModel,
                            onNavigate = { screen -> currentScreen = screen }
                        )
                        Screen.HISTORY -> HistoryScreen(
                            viewModel = viewModel
                        )
                        Screen.BILL_CYCLE -> BillCycleScreen(
                            viewModel = viewModel,
                            onNavigate = { screen -> currentScreen = screen }
                        )
                        Screen.METERS -> MetersScreen(
                            viewModel = viewModel
                        )
                        Screen.REPORTS -> ReportsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
