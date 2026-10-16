package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.BrowseScreen
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ScrapProfilesScreen
import com.example.ui.screens.ScrapProfileMediaScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ProxXQuepusTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProxXQuepusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val isAppLocked by viewModel.isAppLocked.collectAsState()
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    if (isAppLocked) {
                        LockScreen(viewModel = viewModel)
                    } else {
                        // Global BackHandler for nested navigation stack
                        BackHandler(enabled = true) {
                            val handled = viewModel.navigateBack()
                            if (!handled) {
                                finish()
                            }
                        }

                        when (val screen = currentScreen) {
                            is Screen.Browse -> {
                                BrowseScreen(viewModel = viewModel)
                            }
                            is Screen.Collections -> {
                                CollectionsScreen(viewModel = viewModel)
                            }
                            is Screen.CollectionView -> {
                                CollectionDetailScreen(
                                    title = screen.title,
                                    type = screen.type,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                            is Screen.Search -> {
                                SearchScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            is Screen.ScrapProfiles -> {
                                ScrapProfilesScreen(viewModel = viewModel, siteId = screen.siteId)
                            }
                            is Screen.ScrapProfileMedia -> {
                                ScrapProfileMediaScreen(profileId = screen.profileId, viewModel = viewModel)
                            }
                            is Screen.MediaViewer -> {
                                val activeList by viewModel.activeViewerList.collectAsState()
                                MediaViewerScreen(
                                    initialIndex = screen.currentIndex,
                                    mediaList = activeList,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
