package com.example.ui.screens.generator

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiManager
import com.example.data.FirestoreManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(onNavigateBack: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBase64 by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val geminiManager = remember { GeminiManager() }
    val firestoreManager = remember { FirestoreManager() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Generator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe your wallpaper") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isGenerating
            )
            
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        isGenerating = true
                        error = null
                        scope.launch {
                            val result = geminiManager.generateWallpaper(prompt)
                            result.onSuccess { base64 ->
                                generatedBase64 = base64
                                // We won't save base64 directly to Firestore as it's too large for a document usually.
                                // In a real app, upload base64 to Firebase Storage and save URL.
                                // But for MVP without Firebase Storage enabled, we might hit 1MB limit.
                                // We'll just display it for now.
                                // firestoreManager.saveGeneratedWallpaper(base64, prompt, "Custom")
                            }.onFailure { e ->
                                error = e.message
                            }
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating && prompt.isNotBlank()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Generate (1 Credit)")
                }
            }
            
            if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
            
            generatedBase64?.let { base64 ->
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated Wallpaper",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
