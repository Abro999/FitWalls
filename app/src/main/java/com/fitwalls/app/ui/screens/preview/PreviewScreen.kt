package com.fitwalls.app.ui.screens.preview

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.google.firebase.functions.FirebaseFunctions
import com.razorpay.Checkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    var isPurchasingPremium by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var isPremiumUser by remember { mutableStateOf(false) }
    
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
                val bitmap = (result as? BitmapDrawable)?.bitmap ?: result?.toBitmap()
                if (bitmap != null) {
                    val wm = WallpaperManager.getInstance(context)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        wm.setBitmap(bitmap, null, true, targetFlag)
                    } else {
                        wm.setBitmap(bitmap)
                    }
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
        isPremiumUser = firestoreManager.isPremiumMember()
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
                if (isPurchasingPremium || PaymentBus.pendingPaymentType == PaymentBus.TYPE_PREMIUM_PASS) {
                    isPurchasingPremium = false
                    PaymentBus.pendingPaymentType = null
                    val success = firestoreManager.markUserAsPremium()
                    if (success) {
                        isPremiumUser = true
                        Toast.makeText(context, "Premium Pass activated! You can now apply this wallpaper.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error activating premium. Please restart app.", Toast.LENGTH_LONG).show()
                    }
                } else {
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
        }
        launch {
            PaymentBus.paymentErrorFlow.collect { (code, response) ->
                isProcessingPayment = false
                isPurchasingPremium = false
                PaymentBus.pendingPaymentType = null
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
                        Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                            Text(wp.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("By ${wp.creatorName}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                if (wp.pricingType == "Premium-only") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isPremiumUser) "PREMIUM (UNLOCKED)" else "PREMIUM",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        val isPaidLocked = wp.pricingType == "Paid" && !hasPurchased
                        val isPremiumLocked = wp.pricingType == "Premium-only" && !isPremiumUser

                        if (isPremiumLocked) {
                            Button(
                                onClick = {
                                    if (activity == null) return@Button
                                    val checkout = Checkout()
                                    checkout.setKeyID(Constants.RAZORPAY_KEY_ID)
                                    try {
                                        val options = JSONObject()
                                        options.put("name", "FitWalls")
                                        options.put("description", "FitWalls Premium Lifetime Pass")
                                        options.put("currency", "INR")
                                        options.put("amount", Constants.PREMIUM_PASS_PRICE_PAISE)

                                        val user = FirebaseAuth.getInstance().currentUser
                                        user?.email?.let { options.put("prefill.email", it) }
                                        user?.phoneNumber?.let { options.put("prefill.contact", it) }

                                        isPurchasingPremium = true
                                        PaymentBus.pendingPaymentType = PaymentBus.TYPE_PREMIUM_PASS
                                        checkout.open(activity, options)
                                    } catch (e: Exception) {
                                        isPurchasingPremium = false
                                        PaymentBus.pendingPaymentType = null
                                        Toast.makeText(context, "Error opening payment gateway: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isPurchasingPremium
                            ) {
                                if (isPurchasingPremium) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Get Premium to unlock — ₹${Constants.PREMIUM_PASS_PRICE_RUPEES}")
                                }
                            }
                        } else if (isPaidLocked) {
                            Button(
                                onClick = {
                                    if (activity == null) return@Button

                                    val isCreatorUpload = wp.creatorId != "admin" && wp.creatorId.isNotBlank()
                                    val amountInPaise = (wp.price * 100).toInt()

                                    if (isCreatorUpload) {
                                        // Creator upload -> Call Cloud Function "createSplitOrder" for Razorpay Route split payment
                                        scope.launch {
                                            isProcessingPayment = true
                                            try {
                                                val functions = FirebaseFunctions.getInstance()
                                                val requestData = hashMapOf(
                                                    "wallpaperId" to wp.id,
                                                    "amountInPaise" to amountInPaise
                                                )
                                                val result = functions.getHttpsCallable("createSplitOrder")
                                                    .call(requestData)
                                                    .await()

                                                val resultData = result.data as? Map<*, *>
                                                val orderId = (resultData?.get("orderId")
                                                    ?: resultData?.get("order_id")
                                                    ?: resultData?.get("id")) as? String

                                                if (orderId.isNullOrBlank()) {
                                                    isProcessingPayment = false
                                                    Toast.makeText(
                                                        context,
                                                        "Unable to create split order. Please try again.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    return@launch
                                                }

                                                val checkout = Checkout()
                                                checkout.setKeyID(Constants.RAZORPAY_KEY_ID)
                                                val options = JSONObject()
                                                options.put("name", "FitWalls")
                                                options.put("description", "Purchase ${wp.title}")
                                                options.put("currency", "INR")
                                                options.put("amount", amountInPaise)
                                                options.put("order_id", orderId)

                                                val user = FirebaseAuth.getInstance().currentUser
                                                user?.email?.let {
                                                    options.put("prefill.email", it)
                                                }
                                                user?.phoneNumber?.let {
                                                    options.put("prefill.contact", it)
                                                }

                                                checkout.open(activity, options)
                                            } catch (e: Exception) {
                                                isProcessingPayment = false
                                                val errorMsg = e.localizedMessage ?: e.message ?: "Failed to initiate payment"
                                                Toast.makeText(context, "Payment error: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        // Admin upload or direct checkout
                                        val checkout = Checkout()
                                        checkout.setKeyID(Constants.RAZORPAY_KEY_ID)
                                        try {
                                            val options = JSONObject()
                                            options.put("name", "FitWalls")
                                            options.put("description", "Purchase ${wp.title}")
                                            options.put("currency", "INR")
                                            options.put("amount", amountInPaise)

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
