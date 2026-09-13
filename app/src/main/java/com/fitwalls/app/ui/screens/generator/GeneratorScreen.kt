package com.fitwalls.app.ui.screens.generator

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fitwalls.app.ai.GeminiManager
import com.fitwalls.app.data.FirestoreManager
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

import com.fitwalls.app.util.WALLPAPER_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(onNavigateBack: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(WALLPAPER_CATEGORIES[0]) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBase64 by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val geminiManager = remember { GeminiManager() }
    val firestoreManager = remember { FirestoreManager() }
    
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // TODO: Implement actual premium check. For now, stubbed to false.
    val isPremiumUser = false
    
    var credits by remember { mutableIntStateOf(1) } // Start with 1 credit
    var mRewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccessfully by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    
    // Function to load rewarded ad
    fun loadRewardedAd() {
        if (isPremiumUser) return
        isAdLoading = true
        // REAL Rewarded Ad Unit ID: ca-app-pub-6916958158520465/3246364999
        val adUnitId = "ca-app-pub-6916958158520465/3246364999"
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
                isAdLoading = false
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                isAdLoading = false
            }
        })
    }
    
    LaunchedEffect(Unit) {
        loadRewardedAd()
    }
    
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

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = !isGenerating
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    WALLPAPER_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Text("Available Credits: $credits", style = MaterialTheme.typography.labelLarge)
            
            Button(
                onClick = {
                    if (prompt.isNotBlank() && credits > 0) {
                        isGenerating = true
                        error = null
                        savedSuccessfully = false
                        saveError = null
                        
                        scope.launch {
                            val result = geminiManager.generateWallpaper(prompt)
                            result.onSuccess { base64 ->
                                generatedBase64 = base64
                                credits -= 1
                                
                                isSaving = true
                                val success = firestoreManager.saveGeneratedWallpaper(base64, prompt, selectedCategory)
                                isSaving = false
                                
                                if (success) {
                                    savedSuccessfully = true
                                } else {
                                    saveError = "Failed to upload to gallery"
                                }
                            }.onFailure { e ->
                                error = e.message
                            }
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating && prompt.isNotBlank() && credits > 0
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Generate (1 Credit)")
                }
            }
            
            if (!isPremiumUser) {
                OutlinedButton(
                    onClick = {
                        if (mRewardedAd != null && activity != null) {
                            mRewardedAd?.show(activity) { rewardItem ->
                                // Reward the user!
                                credits += 1
                                // Load the next ad
                                loadRewardedAd()
                            }
                        } else {
                            loadRewardedAd()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating && !isAdLoading
                ) {
                    if (isAdLoading && mRewardedAd == null) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading Ad...")
                    } else {
                        Text("Watch ad for +1 free AI credit")
                    }
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
                
                Spacer(modifier = Modifier.height(8.dp))
                if (isSaving) {
                    Text("Saving to your gallery...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (savedSuccessfully) {
                    Text("Saved to your gallery ✓", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                } else if (saveError != null) {
                    Text("Error: $saveError", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
