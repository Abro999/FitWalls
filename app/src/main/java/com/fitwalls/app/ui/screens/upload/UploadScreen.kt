package com.fitwalls.app.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.fitwalls.app.data.FirestoreManager
import kotlinx.coroutines.launch
import java.io.InputStream

import com.fitwalls.app.util.WALLPAPER_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(onNavigateBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(WALLPAPER_CATEGORIES[0]) }
    
    val pricingOptions = listOf("Free", "Paid", "Premium-only")
    var selectedPricing by remember { mutableStateOf(pricingOptions[0]) }
    var priceInput by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreManager = remember { FirestoreManager() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Wallpaper") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Image Picker Area
            Card(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to select image")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

            // Pricing Selector
            Text("Pricing Type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                pricingOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedPricing == option,
                        onClick = { selectedPricing = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = pricingOptions.size)
                    ) {
                        Text(option)
                    }
                }
            }

            if (selectedPricing == "Paid") {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { 
                        // Allow only numbers and maybe a decimal
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            priceInput = it
                        }
                    },
                    label = { Text("Price (₹20 - ₹500) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uploadStatus != null) {
                Text(
                    text = uploadStatus!!,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val priceVal = priceInput.toDoubleOrNull() ?: 0.0
                    if (selectedImageUri == null) {
                        uploadStatus = "Please select an image."
                        isSuccess = false
                        return@Button
                    }
                    if (title.isBlank()) {
                        uploadStatus = "Title cannot be empty."
                        isSuccess = false
                        return@Button
                    }
                    if (selectedPricing == "Paid" && (priceVal < 20.0 || priceVal > 500.0)) {
                        uploadStatus = "Price must be between ₹20 and ₹500."
                        isSuccess = false
                        return@Button
                    }

                    isUploading = true
                    uploadStatus = null

                    scope.launch {
                        try {
                            val inputStream: InputStream? = context.contentResolver.openInputStream(selectedImageUri!!)
                            if (inputStream != null) {
                                val bytes = inputStream.readBytes()
                                inputStream.close()
                                
                                val success = firestoreManager.uploadCreatorWallpaper(
                                    imageBytes = bytes,
                                    title = title,
                                    category = selectedCategory,
                                    pricingType = selectedPricing,
                                    price = if (selectedPricing == "Paid") priceVal else 0.0
                                )
                                
                                if (success) {
                                    isSuccess = true
                                    uploadStatus = "Upload successful!"
                                    // Could add delay here before popping back
                                    onNavigateBack()
                                } else {
                                    isSuccess = false
                                    uploadStatus = "Failed to upload. Please try again."
                                }
                            } else {
                                isSuccess = false
                                uploadStatus = "Could not read image file."
                            }
                        } catch (e: Exception) {
                            isSuccess = false
                            uploadStatus = "Error: ${e.message}"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Upload Wallpaper")
                }
            }
        }
    }
}
