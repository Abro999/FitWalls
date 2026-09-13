package com.fitwalls.app.ui.screens.upload

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import com.fitwalls.app.util.WALLPAPER_CATEGORIES
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWallpaperScreen(
    wallpaperId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreManager = remember { FirestoreManager() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(WALLPAPER_CATEGORIES[0]) }

    val pricingOptions = listOf("Free", "Paid", "Premium-only")
    var selectedPricing by remember { mutableStateOf(pricingOptions[0]) }
    var priceInput by remember { mutableStateOf("") }

    LaunchedEffect(wallpaperId) {
        val wp = firestoreManager.getWallpaper(wallpaperId)
        if (wp != null) {
            if (currentUser == null || wp.creatorId != currentUser.uid) {
                Toast.makeText(context, "Unauthorized to edit this wallpaper", Toast.LENGTH_SHORT).show()
                onNavigateBack()
                return@LaunchedEffect
            }
            wallpaper = wp
            title = wp.title
            if (wp.style in WALLPAPER_CATEGORIES) {
                selectedCategory = wp.style
            }
            if (wp.pricingType in pricingOptions) {
                selectedPricing = wp.pricingType
            }
            priceInput = if (wp.price > 0) wp.price.toString() else ""
        } else {
            Toast.makeText(context, "Wallpaper not found", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isUpdating && !isDeleting && wallpaper != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Wallpaper", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (wallpaper != null) {
            val wp = wallpaper!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wallpaper preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = wp.imageUrl,
                        contentDescription = wp.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        WALLPAPER_CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Pricing Type
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Pricing Type", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pricingOptions.forEach { option ->
                            FilterChip(
                                selected = selectedPricing == option,
                                onClick = { selectedPricing = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                // Price Input (if Paid)
                if (selectedPricing == "Paid") {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Price (INR)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save Changes Button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val price = if (selectedPricing == "Paid") {
                            priceInput.toDoubleOrNull() ?: 0.0
                        } else 0.0

                        if (selectedPricing == "Paid" && price <= 0.0) {
                            Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isUpdating = true
                            val updates = mapOf(
                                "title" to title.trim(),
                                "style" to selectedCategory,
                                "pricingType" to selectedPricing,
                                "price" to price
                            )
                            val success = firestoreManager.updateWallpaper(wp.id, updates)
                            isUpdating = false
                            if (success) {
                                Toast.makeText(context, "Wallpaper updated successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Failed to update wallpaper", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating && !isDeleting
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Changes")
                    }
                }

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !isUpdating && !isDeleting
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Wallpaper")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Wallpaper") },
            text = {
                if (isDeleting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Deleting wallpaper...")
                    }
                } else {
                    Text("Are you sure you want to permanently delete '${title.ifBlank { "this wallpaper" }}'? This action cannot be undone.")
                }
            },
            confirmButton = {
                if (!isDeleting) {
                    Button(
                        onClick = {
                            val wp = wallpaper ?: return@Button
                            scope.launch {
                                isDeleting = true
                                val success = firestoreManager.deleteWallpaper(wp.id)
                                isDeleting = false
                                showDeleteDialog = false
                                if (success) {
                                    Toast.makeText(context, "Wallpaper deleted successfully", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Failed to delete wallpaper", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                if (!isDeleting) {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
