package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ElectricMeter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val urduTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD(
        title = "Dashboard",
        urduTitle = "ڈیش بورڈ",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    ADD_READING(
        title = "Add Reading",
        urduTitle = "ریڈنگ درج کریں",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    ),
    HISTORY(
        title = "History",
        urduTitle = "تاریخچہ",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    BILL_CYCLE(
        title = "Bill Cycle",
        urduTitle = "بل سائیکل",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    METERS(
        title = "Meters",
        urduTitle = "میٹرز",
        selectedIcon = Icons.Filled.ElectricMeter,
        unselectedIcon = Icons.Outlined.ElectricMeter
    ),
    REPORTS(
        title = "Reports",
        urduTitle = "رپورٹس",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    ),
    ADMIN(
        title = "Admin",
        urduTitle = "ایڈمن",
        selectedIcon = Icons.Filled.AdminPanelSettings,
        unselectedIcon = Icons.Outlined.AdminPanelSettings
    )
}
