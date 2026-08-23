package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ads.AdManager
import com.example.data.models.AdminSettings
import com.example.data.models.ButtonAdConfig
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 1. Authorization Gate (Double Check Client Side)
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val userEmail = currentUser?.email ?: ""
    val isAdmin = userEmail.lowercase() == "techsadaqat@gmail.com"

    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Access Denied",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are not authorized to view this page. Only the designated administrator account (TechSadaqat@gmail.com) can access remote configuration.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current session: $userEmail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
        return
    }

    // 2. Admin Local Working States (Initialized from current AdManager cache)
    val adminSettingsState by AdManager.adminSettingsFlow.collectAsStateWithLifecycle(initialValue = null)
    var isInitialized by remember { mutableStateOf(false) }

    var adsEnabled by remember { mutableStateOf(AdManager.adsEnabled) }
    var guestAdsEnabled by remember { mutableStateOf(AdManager.guestAdsEnabled) }
    var googleUserAdsEnabled by remember { mutableStateOf(AdManager.googleUserAdsEnabled) }
    var bannerAdsEnabled by remember { mutableStateOf(AdManager.bannerAdsEnabled) }
    var interstitialAdsEnabled by remember { mutableStateOf(AdManager.interstitialAdsEnabled) }
    var rewardedAdsEnabled by remember { mutableStateOf(AdManager.rewardedAdsEnabled) }

    var bannerAdUnitId by remember { mutableStateOf(AdManager.bannerAdUnitId) }
    var interstitialAdUnitId by remember { mutableStateOf(AdManager.interstitialAdUnitId) }
    var rewardedAdUnitId by remember { mutableStateOf(AdManager.rewardedAdUnitId) }
    var admobAppId by remember { mutableStateOf(AdManager.admobAppId) }

    var interstitialIntervalMinutes by remember { mutableFloatStateOf(AdManager.interstitialIntervalMinutes.toFloat()) }
    var oneMonthMemberEnabled by remember { mutableStateOf(AdManager.oneMonthMemberEnabled) }
    var oneMonthMemberTargetAds by remember { androidx.compose.runtime.mutableIntStateOf(AdManager.oneMonthMemberTargetAds) }

    LaunchedEffect(adminSettingsState) {
        val settings = adminSettingsState
        if (settings != null && !isInitialized) {
            adsEnabled = settings.adsEnabled
            guestAdsEnabled = settings.guestAdsEnabled
            googleUserAdsEnabled = settings.googleUserAdsEnabled
            bannerAdsEnabled = settings.bannerAdsEnabled
            interstitialAdsEnabled = settings.interstitialAdsEnabled
            rewardedAdsEnabled = settings.rewardedAdsEnabled
            bannerAdUnitId = settings.bannerAdUnitId
            interstitialAdUnitId = settings.interstitialAdUnitId
            rewardedAdUnitId = settings.rewardedAdUnitId
            admobAppId = settings.admobAppId
            interstitialIntervalMinutes = settings.interstitialIntervalMinutes.toFloat()
            oneMonthMemberEnabled = settings.oneMonthMemberEnabled
            oneMonthMemberTargetAds = settings.oneMonthMemberTargetAds
            isInitialized = true
        }
    }

    var selectedUserType by remember { mutableStateOf("guest") }

    // List of standard button IDs managed
    val buttonsToManage = remember {
        listOf(
            // Primary actions
            ButtonAdConfig("add_reading", "Save Daily Reading", "Add Reading Screen", "rewarded", true),
            ButtonAdConfig("save_meter", "Save New Meter", "Meters Screen", "rewarded", true),
            ButtonAdConfig("save_bill_cycle", "Save Bill Cycle", "Bill Cycle Screen", "interstitial", true),
            ButtonAdConfig("history_actions", "History Export/Share", "History Screen", "interstitial", true),
            ButtonAdConfig("profile_actions", "Profile Actions", "Profile Dialog", "interstitial", true),
            ButtonAdConfig("cloud_sync_actions", "Cloud Sync Actions", "Data Sync Dialog", "interstitial", true),
            
            // Tab clicked transition ads
            ButtonAdConfig("tab_dashboard", "Dashboard Tab Click", "Tab Transitions", "none", true),
            ButtonAdConfig("tab_add_reading", "Add Reading Tab Click", "Tab Transitions", "none", true),
            ButtonAdConfig("tab_history", "History Tab Click", "Tab Transitions", "none", true),
            ButtonAdConfig("tab_bill_cycle", "Bill Cycle Tab Click", "Tab Transitions", "none", true),
            ButtonAdConfig("tab_meters", "Meters Tab Click", "Tab Transitions", "none", true),
            ButtonAdConfig("tab_reports", "Reports Tab Click", "Tab Transitions", "none", true),
            
            // Tab banner placements
            ButtonAdConfig("banner_dashboard", "Dashboard Banner Display", "Banner Placements", "banner", true),
            ButtonAdConfig("banner_add_reading", "Add Reading Banner Display", "Banner Placements", "banner", true),
            ButtonAdConfig("banner_history", "History Banner Display", "Banner Placements", "banner", true),
            ButtonAdConfig("banner_bill_cycle", "Bill Cycle Banner Display", "Banner Placements", "banner", true),
            ButtonAdConfig("banner_meters", "Meters Banner Display", "Banner Placements", "banner", true),
            ButtonAdConfig("banner_reports", "Reports Banner Display", "Banner Placements", "banner", true),
            
            // Banner click actions
            ButtonAdConfig("click_banner_dashboard", "Dashboard Banner Click action", "Banner Clicks", "none", true),
            ButtonAdConfig("click_banner_add_reading", "Add Reading Banner Click action", "Banner Clicks", "none", true),
            ButtonAdConfig("click_banner_history", "History Banner Click action", "Banner Clicks", "none", true),
            ButtonAdConfig("click_banner_bill_cycle", "Bill Cycle Banner Click action", "Banner Clicks", "none", true),
            ButtonAdConfig("click_banner_meters", "Meters Banner Click action", "Banner Clicks", "none", true),
            ButtonAdConfig("click_banner_reports", "Reports Banner Click action", "Banner Clicks", "none", true)
        )
    }

    val localButtonConfigs = remember { mutableStateMapOf<String, ButtonAdConfig>() }
    
    val remoteConfigsState by AdManager.buttonConfigsFlow.collectAsStateWithLifecycle(initialValue = AdManager.getButtonConfigs())

    LaunchedEffect(selectedUserType, remoteConfigsState) {
        buttonsToManage.forEach { default ->
            val docId = "${default.buttonId}_$selectedUserType"
            val current = remoteConfigsState[docId] ?: remoteConfigsState[default.buttonId]
            if (current != null) {
                localButtonConfigs[default.buttonId] = current.copy(buttonId = default.buttonId)
            } else {
                localButtonConfigs[default.buttonId] = default
            }
        }
    }

    // Force synchronization state on screen entry/refresh
    var isSaving by remember { mutableStateOf(false) }

    fun saveButtonConfigToFirebase(
        buttonId: String,
        config: ButtonAdConfig,
        userType: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val docId = "${buttonId}_$userType"
        val btnRef = db.collection("button_ad_configs").document(docId)
        val btnMap = mapOf(
            "button_id" to docId,
            "button_name" to config.buttonName,
            "screen_name" to config.screenName,
            "ad_type" to config.adType,
            "enabled" to config.enabled,
            "updated_at" to System.currentTimeMillis(),
            "updated_by" to (currentUser?.email ?: "TechSadaqat@gmail.com")
        )
        btnRef.set(btnMap)
            .addOnSuccessListener {
                android.util.Log.d("AdminPanel", "Successfully saved granular config for $docId")
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("AdminPanel", "Error saving granular config for $docId", exception)
            }
    }

    fun saveToFirebase(
        newAdsEnabled: Boolean = adsEnabled,
        newGuestAdsEnabled: Boolean = guestAdsEnabled,
        newGoogleUserAdsEnabled: Boolean = googleUserAdsEnabled,
        newBannerAdsEnabled: Boolean = bannerAdsEnabled,
        newInterstitialAdsEnabled: Boolean = interstitialAdsEnabled,
        newRewardedAdsEnabled: Boolean = rewardedAdsEnabled,
        newBannerAdUnitId: String = bannerAdUnitId,
        newInterstitialAdUnitId: String = interstitialAdUnitId,
        newRewardedAdUnitId: String = rewardedAdUnitId,
        newAdmobAppId: String = admobAppId,
        newInterstitialIntervalMinutes: Float = interstitialIntervalMinutes,
        newOneMonthMemberEnabled: Boolean = oneMonthMemberEnabled,
        newOneMonthMemberTargetAds: Int = oneMonthMemberTargetAds
    ) {
        val db = FirebaseFirestore.getInstance()
        val settingsRef = db.collection("config").document("admin_settings")
        val settingsMap = mapOf(
            "admin_email" to "TechSadaqat@gmail.com",
            "ads_enabled" to newAdsEnabled,
            "guest_ads_enabled" to newGuestAdsEnabled,
            "google_user_ads_enabled" to newGoogleUserAdsEnabled,
            "banner_ads_enabled" to newBannerAdsEnabled,
            "interstitial_ads_enabled" to newInterstitialAdsEnabled,
            "rewarded_ads_enabled" to newRewardedAdsEnabled,
            "banner_ad_unit_id" to newBannerAdUnitId,
            "interstitial_ad_unit_id" to newInterstitialAdUnitId,
            "rewarded_ad_unit_id" to newRewardedAdUnitId,
            "admob_app_id" to newAdmobAppId,
            "interstitial_interval_minutes" to newInterstitialIntervalMinutes.toInt(),
            "one_month_member_enabled" to newOneMonthMemberEnabled,
            "one_month_member_target_ads" to newOneMonthMemberTargetAds,
            "updated_at" to System.currentTimeMillis(),
            "updated_by" to (currentUser?.email ?: "TechSadaqat@gmail.com")
        )
        settingsRef.set(settingsMap)
            .addOnSuccessListener {
                android.util.Log.d("AdminPanel", "Auto-saved global settings to cloud Firestore")
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("AdminPanel", "Error auto-saving global settings", exception)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Quick Refresh Button
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Cache",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                            .clickable {
                                isInitialized = false
                                adsEnabled = AdManager.adsEnabled
                                guestAdsEnabled = AdManager.guestAdsEnabled
                                googleUserAdsEnabled = AdManager.googleUserAdsEnabled
                                bannerAdsEnabled = AdManager.bannerAdsEnabled
                                interstitialAdsEnabled = AdManager.interstitialAdsEnabled
                                rewardedAdsEnabled = AdManager.rewardedAdsEnabled
                                bannerAdUnitId = AdManager.bannerAdUnitId
                                interstitialAdUnitId = AdManager.interstitialAdUnitId
                                rewardedAdUnitId = AdManager.rewardedAdUnitId
                                admobAppId = AdManager.admobAppId
                                interstitialIntervalMinutes = AdManager.interstitialIntervalMinutes.toFloat()
                                oneMonthMemberEnabled = AdManager.oneMonthMemberEnabled
                                oneMonthMemberTargetAds = AdManager.oneMonthMemberTargetAds

                                buttonsToManage.forEach { default ->
                                    val current = AdManager.getButtonConfigs()[default.buttonId] ?: default
                                    localButtonConfigs[default.buttonId] = current
                                }
                                Toast.makeText(context, "Admin settings synchronized with cache!", Toast.LENGTH_SHORT).show()
                            }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricBlue)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Authorized Admin Info Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SafetyCheck,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Authenticated Administrator",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricBlue
                        )
                        Text(
                            text = "Active session: TechSadaqat@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SECTION 1: GLOBAL ADS & FORMAT MASTER SWITCHES
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Global Ad System",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (adsEnabled) ElectricBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Completely enable or disable all ads",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = { 
                                adsEnabled = it 
                                saveToFirebase(newAdsEnabled = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("global_ads_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = adsEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Ad Formats
                            Text(
                                text = "Ad Formats Controls",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Banner Ads Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Banner Advertisements",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = bannerAdsEnabled,
                                    onCheckedChange = { 
                                        bannerAdsEnabled = it 
                                        saveToFirebase(newBannerAdsEnabled = it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                                )
                            }

                            // Interstitial Ads Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Interstitial Advertisements",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = interstitialAdsEnabled,
                                    onCheckedChange = { 
                                        interstitialAdsEnabled = it 
                                        saveToFirebase(newInterstitialAdsEnabled = it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                                )
                            }

                            // Rewarded Video Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rewarded Video Advertisements",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = rewardedAdsEnabled,
                                    onCheckedChange = { 
                                        rewardedAdsEnabled = it 
                                        saveToFirebase(newRewardedAdsEnabled = it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // User groups rules
                            Text(
                                text = "Allowed Target User Categories",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Guest Ads Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ads on Guest Users (Signed-out)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = guestAdsEnabled,
                                    onCheckedChange = { 
                                        guestAdsEnabled = it 
                                        saveToFirebase(newGuestAdsEnabled = it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                                )
                            }

                            // Google Users Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ads on Google Logged-in Users",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = googleUserAdsEnabled,
                                    onCheckedChange = { 
                                        googleUserAdsEnabled = it 
                                        saveToFirebase(newGoogleUserAdsEnabled = it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: INTERSTITIAL FREQUENCY (COOLDOWN)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interstitial Frequency Interval",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Controls global cooldown frequency between interstitial ads to protect user experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interval Duration:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${interstitialIntervalMinutes.toInt()} minutes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricBlue
                            )
                        )
                    }

                    Slider(
                        value = interstitialIntervalMinutes,
                        onValueChange = { interstitialIntervalMinutes = it },
                        onValueChangeFinished = {
                            saveToFirebase(newInterstitialIntervalMinutes = interstitialIntervalMinutes)
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = ElectricBlue.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("5 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("10 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // SECTION 3: ONE MONTH MEMBER TOGGLE
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (oneMonthMemberEnabled) SuccessGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "One Month Member Feature",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Allow Google user to earn one month ad exemption by watching $oneMonthMemberTargetAds rewarded ads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (oneMonthMemberEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Target ads count:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Button(
                                        onClick = {
                                            if (oneMonthMemberTargetAds > 1) {
                                                val newCount = oneMonthMemberTargetAds - 1
                                                oneMonthMemberTargetAds = newCount
                                                saveToFirebase(newOneMonthMemberTargetAds = newCount)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    
                                    Text(
                                        text = "$oneMonthMemberTargetAds",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = ElectricBlue,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val newCount = oneMonthMemberTargetAds + 1
                                            oneMonthMemberTargetAds = newCount
                                            saveToFirebase(newOneMonthMemberTargetAds = newCount)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        Switch(
                            checked = oneMonthMemberEnabled,
                            onCheckedChange = { 
                                oneMonthMemberEnabled = it 
                                saveToFirebase(newOneMonthMemberEnabled = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen
                            )
                        )
                    }
                }
            }

            // SECTION 4: GRANULAR BUTTON-LEVEL AD CONFIGURATIONS
            Text(
                text = "Granular Action Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ElectricBlue,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Two Radio Buttons for Guest vs Google sign-in user selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedUserType = "guest" }
                ) {
                    RadioButton(
                        selected = (selectedUserType == "guest"),
                        onClick = { selectedUserType = "guest" },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedUserType == "guest") ElectricBlue else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedUserType = "google_user" }
                ) {
                    RadioButton(
                        selected = (selectedUserType == "google_user"),
                        onClick = { selectedUserType = "google_user" },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Google sign-in user",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedUserType == "google_user") ElectricBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // All buttons toggle below radio buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "All buttons (Toggle all on/off)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val allEnabled = localButtonConfigs.values.all { it.enabled }
                Switch(
                    checked = allEnabled,
                    onCheckedChange = { enableAll ->
                        buttonsToManage.forEach { default ->
                            val current = localButtonConfigs[default.buttonId] ?: default
                            val updated = current.copy(enabled = enableAll)
                            localButtonConfigs[default.buttonId] = updated
                            saveButtonConfigToFirebase(default.buttonId, updated, selectedUserType)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            localButtonConfigs.forEach { (buttonId, config) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.enabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (config.enabled) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.buttonName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (config.enabled) ElectricBlue else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Screen: ${config.screenName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { isEnabled ->
                                    val updatedConfig = config.copy(enabled = isEnabled)
                                    localButtonConfigs[buttonId] = updatedConfig
                                    saveButtonConfigToFirebase(buttonId, updatedConfig, selectedUserType)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue),
                                modifier = Modifier.testTag("switch_$buttonId")
                            )
                        }

                        AnimatedVisibility(
                            visible = config.enabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Add format:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                val formats = listOf("none", "banner", "interstitial", "rewarded")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    formats.forEach { format ->
                                        val isSelected = config.adType == format
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                val updatedConfig = config.copy(adType = format)
                                                localButtonConfigs[buttonId] = updatedConfig
                                                saveButtonConfigToFirebase(buttonId, updatedConfig, selectedUserType)
                                            },
                                            label = {
                                                Text(
                                                    text = format.replaceFirstChar { it.uppercase() },
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricBlue,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: ADMOB AD UNIT ID OVERRIDES
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dataset,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AdMob Ad Unit IDs Overrides",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Specify custom production Ad Unit IDs. Leave blank to fallback to SDK default test IDs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. AdMob App ID
                    OutlinedTextField(
                        value = admobAppId,
                        onValueChange = {
                            admobAppId = it
                        },
                        label = { Text("AdMob App ID") },
                        placeholder = { Text("e.g. ca-app-pub-3940256099942544~3347511713") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            focusedLabelColor = ElectricBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_admob_app_id")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Banner ID
                    OutlinedTextField(
                        value = bannerAdUnitId,
                        onValueChange = {
                            bannerAdUnitId = it
                        },
                        label = { Text("AdMob Banner Ad Unit ID") },
                        placeholder = { Text("e.g. ca-app-pub-3940256099942544/6300978111") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            focusedLabelColor = ElectricBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_banner_ad_unit")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Interstitial ID
                    OutlinedTextField(
                        value = interstitialAdUnitId,
                        onValueChange = {
                            interstitialAdUnitId = it
                        },
                        label = { Text("Interstitial Ad Unit ID") },
                        placeholder = { Text("e.g. ca-app-pub-3940256099942544/1033173712") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            focusedLabelColor = ElectricBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_interstitial_ad_unit")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Rewarded ID
                    OutlinedTextField(
                        value = rewardedAdUnitId,
                        onValueChange = {
                            rewardedAdUnitId = it
                        },
                        label = { Text("Rewarded Ad Unit ID") },
                        placeholder = { Text("e.g. ca-app-pub-3940256099942544/5224354917") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            focusedLabelColor = ElectricBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rewarded_ad_unit")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            saveToFirebase(
                                newAdmobAppId = admobAppId,
                                newBannerAdUnitId = bannerAdUnitId,
                                newInterstitialAdUnitId = interstitialAdUnitId,
                                newRewardedAdUnitId = rewardedAdUnitId
                            )
                            Toast.makeText(context, "AdMob Overrides saved to Firebase!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_admob_overrides")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save AdMob Overrides", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
