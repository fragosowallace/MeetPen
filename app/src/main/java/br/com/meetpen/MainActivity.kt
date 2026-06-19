package br.com.meetpen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.meetpen.ui.MainViewModel
import br.com.meetpen.ui.screens.*
import br.com.meetpen.ui.theme.MeetPenTheme
import br.com.meetpen.ui.theme.MidnightBg

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val startDestination = if (intent?.action == "br.com.meetpen.ACTION_START_RECORDING") {
            "recording"
        } else {
            "home"
        }

        setContent {
            MeetPenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightBg
                ) {
                    val viewModel: MainViewModel = viewModel()
                    MeetPenApp(viewModel, startDestination)
                }
            }
        }
    }
}

@Composable
fun MeetPenApp(viewModel: MainViewModel, startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { 
            HomeScreen(
                viewModel = viewModel,
                onStartRecording = { navController.navigate("recording") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToPaywall = { navController.navigate("paywall") },
                onNavigateToDetail = { id -> navController.navigate("note_detail/$id") }
            ) 
        }
        composable("recording") { 
            RecordingScreen(
                viewModel = viewModel,
                onStopRecording = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            ) 
        }
        composable("settings") { 
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) 
        }
        composable("paywall") {
            PaywallScreen(
                onBack = { navController.popBackStack() },
                onSubscribe = { 
                    viewModel.subscribe()
                    navController.popBackStack()
                }
            )
        }
        composable("note_detail/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull() ?: 0
            NoteDetailScreen(
                noteId = noteId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
    }
}
