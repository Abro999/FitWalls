package com.fitwalls.app.ui.screens.preview

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import com.fitwalls.app.util.Constants
import com.fitwalls.app.util.PaymentBus
import com.fitwalls.app.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(wallpaperId: String, onNavigateBack: () -> Unit) {
    val firestoreManager = remember { FirestoreManager() }
    var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var hasPurchased by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    
    // TODO: Implement actual premium check. For now, stubbed to false.
    val isPremiumUser = false
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    
    fun applyWallpaper(targetFlag: Int) {
        val wp = wallpaper ?: return
        scope.launch(Dispatchers.IO) {
            isApplying = true
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(wp.imageUrl)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val wm = WallpaperManager.getInstance(context)
                    wm.setBitmap(bitmap, null, true, targetFlag)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
                        showApplyDialog = false
                        isApplying = false
                        AdManager.onWallpaperApplied(activity, isPremiumUser)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to decode wallpaper image", Toast.LENGTH_SHORT).show()
                        isApplying = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error applying wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                    isApplying = false
                }
            }
        }
    }
    
    // Load wallpaper data and preload interstitial ad
    LaunchedEffect(wallpaperId) {
        AdManager.loadInterstitialAd(context, isPremiumUser)
        wallpaper = firestoreManager.getWallpaper(wallpaperId)
        if (wallpaper != null) {
            hasPurchased = firestoreManager.hasUserPurchased(wallpaperId)
        }
        isLoading = false
    }
    
    // Listen for payment events
    LaunchedEffect(Unit) {
        launch {
            PaymentBus.paymentSuccessFlow.collect { paymentId ->
                isProcessingPayment = true
                val wp = wallpaper
                if (wp != null) {
                    val success = firestoreManager.recordPurchase(
                        wallpaperId = wp.id,
                        creatorId = wp.creatorId,
                        pricePaid = wp.price,
                        razorpayPaymentId = paymentId
                    )
                    if (success) {
                        hasPurchased = true
                        Toast.makeText(context, "Purchase Successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error verifying purchase. Contact support.", Toast.LENGTH_LONG).show()
                    }
                }
                isProcessingPayment = false
            }
        }
        launch {
            PaymentBus.paymentErrorFlow.collect { (code, response) ->
                isProcessingPayment = false
                Toast.makeText(context, "Payment failed: $response", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wallpaper?.title ?: "Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (wallpaper == null) {
                Text("Wallpaper not found", modifier = Modifier.align(Alignment.Center))
            } else {
                val wp = wallpaper!!
                
                AsyncImage(
                    model = wp.imageUrl,
                    contentDescription = wp.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Bottom bar for actions
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(wp.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                            Text("By ${wp.creatorName}", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        if (wp.pricingType == "Paid" && !hasPurchased) {
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        val checkout = Checkout()
                                        checkout.setKeyID(Constants.RAZORPAY_KEY_ID)
                                        try {
                                            val options = JSONObject()
                                            options.put("name", "FitWalls")
                                            options.put("description", "Purchase ${wp.title}")
                                            options.put("currency", "INR")
                                            options.put("amount", (wp.price * 100).toInt()) // paise
                                            
                                            val user = FirebaseAuth.getInstance().currentUser
                                            user?.email?.let {
                                                options.put("prefill.email", it)
                                            }
                                            
                                            isProcessingPayment = true
                                            checkout.open(activity, options)
                                        } catch (e: Exception) {
                                            isProcessingPayment = false
                                            Toast.makeText(context, "Error initializing payment", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isProcessingPayment
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Buy for ₹${wp.price}")
                                }
                            }
                        } else {
                            Button(
                                onClick = { showApplyDialog = true },
                                enabled = !isApplying
                            ) {
                                if (isApplying) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { if (!isApplying) showApplyDialog = false },
            title = { Text("Set Wallpaper") },
            text = {
                if (isApplying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Applying wallpaper...")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Where would you like to set this wallpaper?")
                        FilledTonalButton(
                            onClick = { applyWallpaper(WallpaperManager.FLAG_SYSTEM) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Home Screen")
                        }
                        FilledTonalButton(
                            onClick = { applyWallpaper(WallpaperManager.FLAG_LOCK) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lock Screen")
                        }
                        Button(
                            onClick = { applyWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Both Screens")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!isApplying) {
                    TextButton(onClick = { showApplyDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
