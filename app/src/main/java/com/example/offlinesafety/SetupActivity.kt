package com.example.offlinesafety

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.offlinesafety.data.AppDatabase
import com.example.offlinesafety.data.User
import com.example.offlinesafety.ui.theme.OfflineSafetyTheme
import kotlinx.coroutines.launch

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflineSafetyTheme {
                Surface(Modifier.fillMaxSize()) {
                    SetupScreen { name, contact -> saveUser(name, contact) }
                }
            }
        }
    }

    private fun saveUser(name: String, contact: String) {
        if (name.isBlank() || contact.isBlank()) {
            Toast.makeText(this, "Please enter name & contact", Toast.LENGTH_SHORT).show()
            return
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            try {
                db.userDao().insert(User(id = 1, name = name, contact = contact))
                runOnUiThread {
                    Toast.makeText(this@SetupActivity, "User saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SetupActivity, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun SetupScreen(onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User Setup", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Emergency contact") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSave(name.trim(), contact.trim()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}
