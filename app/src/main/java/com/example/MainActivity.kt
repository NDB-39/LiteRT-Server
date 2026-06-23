package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions (especially external storage for loading models)
        val permissions = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (ungranted.isNotEmpty()) {
            requestPermissionLauncher.launch(ungranted.toTypedArray())
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ServerAppLayout(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding).padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ServerAppLayout(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pathInput by remember { mutableStateOf(uiState.modelPath) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "LiteRT-LM API Server", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = pathInput,
            onValueChange = { pathInput = it },
            label = { Text("Model Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.loadModel(pathInput) }) {
                Text("Load Model")
            }
        }

        uiState.error?.let {
            Text(text = "Error: $it", color = MaterialTheme.colorScheme.error)
        }

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.startServer() },
                enabled = !uiState.isServerRunning
            ) {
                Text("Start Server")
            }
            Button(
                onClick = { viewModel.stopServer() },
                enabled = uiState.isServerRunning
            ) {
                Text("Stop Server")
            }
        }

        HorizontalDivider()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text("Server: ${if(uiState.isServerRunning) "Running" else "Stopped"}")
                Text("Model Loaded: ${uiState.isModelLoaded}")
                Text("Backend: ${uiState.backend}")
                Text("RAM Usage: ${uiState.ramUsedMb} MB")
                Text("TPS: ${String.format("%.2f", uiState.tokensPerSecond)}")
            }
        }
    }
}
