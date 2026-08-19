package com.fitwalls.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitwalls.app.data.FirestoreManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(onRoleSelected: () -> Unit) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val firestoreManager = remember { FirestoreManager() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Account Type") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "How will you use FitWalls?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            RoleCard(
                title = "I'm a User",
                description = "Browse, preview, and apply amazing AI wallpapers to your device.",
                isSelected = selectedRole == "user",
                onClick = { selectedRole = "user" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            RoleCard(
                title = "I'm a Creator",
                description = "Upload your own designs, share them, and potentially sell premium wallpapers.",
                isSelected = selectedRole == "creator",
                onClick = { selectedRole = "creator" }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Button(
                onClick = {
                    if (selectedRole != null) {
                        isSaving = true
                        error = null
                        scope.launch {
                            val success = firestoreManager.createUserProfile(selectedRole!!)
                            if (success) {
                                onRoleSelected()
                            } else {
                                error = "Failed to save profile. Please try again."
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedRole != null && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
