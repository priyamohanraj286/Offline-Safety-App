package com.example.offlinesafety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.offlinesafety.data.AppDatabase
import com.example.offlinesafety.data.Travel
import com.example.offlinesafety.data.User
import com.example.offlinesafety.ui.theme.OfflineSafetyTheme
import kotlinx.coroutines.launch

class ViewUserActivity : ComponentActivity() {
    private val userState = mutableStateOf<User?>(null)
    private val travelState = mutableStateOf<Travel?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflineSafetyTheme {
                Surface(Modifier.fillMaxSize()) {
                    ViewUserScreen(userState.value, travelState.value)
                }
            }
        }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ViewUserActivity)
            val u = try { db.userDao().getUser() } catch (_: Exception) { null }
            val t = try { db.travelDao().getLatestTravel() } catch (_: Exception) { null }
            userState.value = u
            travelState.value = t

            // re-render
            setContent {
                OfflineSafetyTheme {
                    Surface(Modifier.fillMaxSize()) {
                        ViewUserScreen(userState.value, travelState.value)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewUserScreen(user: User?, travel: Travel?) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("User Information", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (user != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Name: ${user.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Contact: ${user.contact}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Text("No user info saved")
        }

        Spacer(Modifier.height(20.dp))
        Text("Travel Information", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (travel != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Train: ${travel.trainName}")
                    Text("Coach: ${travel.coachNumber}")
                    Text("Seat: ${travel.seatNumber}")
                }
            }
        } else {
            Text("No travel info saved")
        }
    }
}
