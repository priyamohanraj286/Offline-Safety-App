@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.offlinesafety

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.offlinesafety.data.AppDatabase
import com.example.offlinesafety.data.Travel
import com.example.offlinesafety.data.User
import com.example.offlinesafety.ui.theme.OfflineSafetyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    var isAdvertising by mutableStateOf(false)
    var currentUser by mutableStateOf<User?>(null)
    var currentTravel by mutableStateOf<Travel?>(null)

    var showTravelDialog by mutableStateOf(false)
    var dialogTrain by mutableStateOf("")
    var dialogCoach by mutableStateOf("")
    var dialogSeat by mutableStateOf("")

    private val requestBluetoothPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            val ok = map.entries.all { it.value }
            if (!ok) Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions()

        setContent {
            OfflineSafetyTheme {
                Surface(Modifier.fillMaxSize()) {
                    MainContent(this)
                    if (showTravelDialog) {
                        TravelComposeDialog(
                            train = dialogTrain,
                            onTrainChange = { dialogTrain = it },
                            coach = dialogCoach,
                            onCoachChange = { dialogCoach = it },
                            seat = dialogSeat,
                            onSeatChange = { dialogSeat = it },
                            onSave = { t, c, s ->
                                saveTravelDetails(t, c, s)
                                showTravelDialog = false
                            },
                            onDismiss = { showTravelDialog = false }
                        )
                    }
                }
            }
        }

        lifecycleScope.launch { loadUserAndTravel() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { loadUserAndTravel() }
    }

    private suspend fun loadUserAndTravel() {
        val db = AppDatabase.getDatabase(this@MainActivity)
        val u = try { db.userDao().getUser() } catch (_: Exception) { null }
        val t = try { db.travelDao().getLatestTravel() } catch (_: Exception) { null }

        withContext(Dispatchers.Main) {
            currentUser = u
            currentTravel = t
        }

        val now = System.currentTimeMillis()
        val valid = t?.let { now - it.startTime <= 48 * 60 * 60 * 1000L } ?: false
        if (valid) {
            startScanService()
        } else {
            db.travelDao().clearAll()
            withContext(Dispatchers.Main) {
                dialogTrain = ""; dialogCoach = ""; dialogSeat = ""
                showTravelDialog = true
            }
        }
    }

    fun saveTravelDetails(train: String, coach: String, seat: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.travelDao().clearAll()
            val travel = Travel(
                trainName = train,
                coachNumber = coach,
                seatNumber = seat,
                startTime = System.currentTimeMillis()
            )
            db.travelDao().insert(travel)
            withContext(Dispatchers.Main) {
                currentTravel = travel
                startScanService()
                Toast.makeText(this@MainActivity, "Travel saved, scanning started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            requestBluetoothPermissionsLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    // ---------------- Services ----------------
    fun startScanService() {
        val intent = Intent(this, SosService::class.java).apply { action = "START_SCAN" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    fun stopScanService() {
        val intent = Intent(this, SosService::class.java).apply { action = "STOP_SCAN" }
        stopService(intent)
    }

    fun startAdvertiseService() {
        if (currentUser == null) {
            Toast.makeText(this, "Please fill user details first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        val intent = Intent(this, SosService::class.java).apply { action = "START_ADVERTISE" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        isAdvertising = true
    }

    fun stopAdvertiseService() {
        val intent = Intent(this, SosService::class.java).apply { action = "STOP_ADVERTISE" }
        stopService(intent)
        isAdvertising = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelComposeDialog(
    train: String,
    onTrainChange: (String) -> Unit,
    coach: String,
    onCoachChange: (String) -> Unit,
    seat: String,
    onSeatChange: (String) -> Unit,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Travelling?") },
        text = {
            Column {
                OutlinedTextField(value = train, onValueChange = onTrainChange, label = { Text("Train Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = coach, onValueChange = onCoachChange, label = { Text("Coach Number") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = seat, onValueChange = onSeatChange, label = { Text("Seat Number") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(train, coach, seat) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MainContent(activity: MainActivity) {
    val ctx = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menu", fontSize = 20.sp, modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text("User Setup") },
                    selected = false,
                    onClick = {
                        ctx.startActivity(Intent(ctx, SetupActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("View User Info") },
                    selected = false,
                    onClick = {
                        ctx.startActivity(Intent(ctx, ViewUserActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Update Travel Details") },
                    selected = false,
                    onClick = {
                        activity.dialogTrain = ""; activity.dialogCoach = ""; activity.dialogSeat = ""
                        activity.showTravelDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Offline Safety App") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (!activity.isAdvertising) activity.startAdvertiseService()
                        else activity.stopAdvertiseService()
                    },
                    modifier = Modifier.size(200.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(
                        if (!activity.isAdvertising) "SOS" else "STOP",
                        fontSize = 36.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
