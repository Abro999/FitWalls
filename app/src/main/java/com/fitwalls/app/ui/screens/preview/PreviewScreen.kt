package com.fitwalls.app.ui.screens.preview

import android.app.Activity
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
import coil.compose.AsyncImage
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import com.fitwalls.app.util.Constants
import com.fitwalls.app.util.PaymentBus
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(wallpaperId: String, onNavigateBack: () -> Unit) {
    val firestoreManager = remember { FirestoreManager() }
    var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var hasPurchased by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Load wallpaper data
    LaunchedEffect(wallpaperId) {
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
                            Button(onClick = {
                                Toast.makeText(context, "Applied wallpaper successfully!", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }
        }
    }
}
