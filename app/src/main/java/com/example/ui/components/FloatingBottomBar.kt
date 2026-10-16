package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.Screen

@Composable
fun FloatingBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(GlassSurfaceHigh)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isBrowse = currentScreen is Screen.Browse
            val isCollections = currentScreen is Screen.Collections || currentScreen is Screen.CollectionView
            val isSettings = currentScreen is Screen.Settings
            val isScrapProfiles = currentScreen is Screen.ScrapProfiles || currentScreen is Screen.ScrapProfileMedia

            BottomNavItem(
                title = "Browse",
                icon = if (isBrowse) Icons.Default.Folder else Icons.Outlined.Folder,
                isSelected = isBrowse,
                onClick = { onNavigate(Screen.Browse) },
                testTag = "nav_browse"
            )

            BottomNavItem(
                title = "Collections",
                icon = if (isCollections) Icons.Default.GridView else Icons.Outlined.GridView,
                isSelected = isCollections,
                onClick = { onNavigate(Screen.Collections) },
                testTag = "nav_collections"
            )

            BottomNavItem(
                title = "Scrap Profiles",
                icon = if (isScrapProfiles) Icons.Default.Link else Icons.Outlined.Link,
                isSelected = isScrapProfiles,
                onClick = { onNavigate(Screen.ScrapProfiles()) },
                testTag = "nav_scrap_profiles"
            )

            BottomNavItem(
                title = "Settings",
                icon = if (isSettings) Icons.Default.Settings else Icons.Outlined.Settings,
                isSelected = isSettings,
                onClick = { onNavigate(Screen.Settings) },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0x3300E5FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) CyanPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        if (isSelected) {
            Text(
                text = title,
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
