package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RichDarkBg
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val profileState = viewModel.playerProfile.collectAsState()
                val profile = profileState.value

                val snackbarHostState = remember { SnackbarHostState() }
                val uiMessage = viewModel.uiMessage.collectAsState()

                // Display dynamic snackbar alerts from ViewModel operations
                LaunchedEffect(uiMessage.value) {
                    uiMessage.value?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    if (profile == null) {
                        // DB is loading/initializing, display premium gold loader
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(RichDarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = com.example.ui.theme.BrightBrass,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    } else {
                        val navController = rememberNavController()
                        val cues = viewModel.allCues.collectAsState().value
                        val progress = viewModel.careerProgress.collectAsState().value

                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onNavigateToCareer = { navController.navigate("career") },
                                    onNavigateToCues = { navController.navigate("cues") },
                                    onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                                    onNavigateToStore = { navController.navigate("store") },
                                    onNavigateToGamePlay = { navController.navigate("gameplay") }
                                )
                            }

                            composable("career") {
                                CareerLevelsScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    progress = progress,
                                    onBackClick = { navController.popBackStack() },
                                    onNavigateToGamePlay = { navController.navigate("gameplay") }
                                )
                            }

                            composable("gameplay") {
                                GamePlayScreen(
                                    viewModel = viewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("cues") {
                                CueCollectionScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    cues = cues,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("leaderboard") {
                                LeaderboardScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("store") {
                                StoreScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
