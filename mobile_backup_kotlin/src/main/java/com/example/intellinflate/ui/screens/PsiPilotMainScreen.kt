package com.example.intellinflate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.intellinflate.viewmodel.ConnectionStatus
import com.example.intellinflate.viewmodel.NavigationTab
import com.example.intellinflate.viewmodel.PsiPilotUIState
import com.example.intellinflate.viewmodel.PsiPilotViewModel

/**
 * Main IntelliInflate App Screen with Navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsiPilotMainScreen(
    viewModel: PsiPilotViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            PsiPilotTopBar(
                uiState = uiState,
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            if (uiState.isUserLoggedIn) {
                PsiPilotBottomBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectNavigationTab(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Show selected tab content
            when (uiState.selectedTab) {
                NavigationTab.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = { viewModel.selectNavigationTab(NavigationTab.REGISTER) }
                    )
                }
                NavigationTab.REGISTER -> {
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { viewModel.selectNavigationTab(NavigationTab.LOGIN) }
                    )
                }
                NavigationTab.DASHBOARD -> {
                    PsiPilotDashboard(
                        uiState = uiState,
                        onDetectVehicle = { viewModel.detectVehicle() },
                        onNavigateToScan = { viewModel.selectNavigationTab(NavigationTab.TIRE_SCAN) },
                        onNavigateToVehicle = { viewModel.selectNavigationTab(NavigationTab.VEHICLE_DETECTION) }
                    )
                }
                NavigationTab.VEHICLE_DETECTION -> {
                    VehicleDetectionScreen(
                        detectionResult = uiState.vehicleDetectionResult,
                        vehicleProfile = uiState.currentVehicle,
                        onDetectAgain = { viewModel.detectVehicle() },
                        onContinue = { viewModel.selectNavigationTab(NavigationTab.TIRE_SCAN) }
                    )
                }
                NavigationTab.TIRE_SCAN -> {
                    TireScanScreen(
                        tireScanResults = uiState.tireScanResults,
                        onScanTire = { tire -> viewModel.scanTire(tire) },
                        onScanAllTires = { viewModel.scanAllTires() }
                    )
                }
                NavigationTab.HISTORY -> {
                    ServiceHistoryScreen()
                }
            }
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PsiPilotTopBar(
    uiState: PsiPilotUIState,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "IntelliInflate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tire Health Monitoring",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            if (uiState.isUserLoggedIn) {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun PsiPilotBottomBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == NavigationTab.DASHBOARD,
            onClick = { onTabSelected(NavigationTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = selectedTab == NavigationTab.VEHICLE_DETECTION,
            onClick = { onTabSelected(NavigationTab.VEHICLE_DETECTION) },
            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
            label = { Text("Vehicle") }
        )
        NavigationBarItem(
            selected = selectedTab == NavigationTab.TIRE_SCAN,
            onClick = { onTabSelected(NavigationTab.TIRE_SCAN) },
            icon = { Icon(Icons.Default.Scanner, contentDescription = null) },
            label = { Text("Health Scan") }
        )
        NavigationBarItem(
            selected = selectedTab == NavigationTab.HISTORY,
            onClick = { onTabSelected(NavigationTab.HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") }
        )
    }
}

@Composable
private fun ConnectionPromptScreen(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Welcome to IntelliInflate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "AI-Powered Tyre Health Detection System",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Connect to Station")
        }
    }
}

@Composable
private fun ConnectionDialog(
    onConnect: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var ipAddress by remember { mutableStateOf("192.168.4.1") }
    var port by remember { mutableStateOf("80") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to IntelliInflate Station") },
        text = {
            Column {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConnect(ipAddress, port.toIntOrNull() ?: 80)
            }) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Connection Error",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retry Connection")
        }
    }
}

@Composable
private fun ServiceHistoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Service History")
    }
}
