package com.fitwalls.app.ui.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.fitwalls.app.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import androidx.compose.ui.viewinterop.AndroidView
import com.fitwalls.app.util.Constants
import com.fitwalls.app.util.PaymentBus
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.json.JSONObject

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import com.fitwalls.app.util.WALLPAPER_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGenerator: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val firestoreManager = remember { FirestoreManager() }
    var wallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All") + WALLPAPER_CATEGORIES }
    
    val filteredWallpapers = remember(wallpapers, selectedCategory) {
        if (selectedCategory == "All") wallpapers else wallpapers.filter { it.style == selectedCategory }
    }
    
    var userRole by remember { mutableStateOf<String?>("user") }
    var isPremiumUser by remember { mutableStateOf(false) }
    var isPurchasingPremium by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLoading = true
                scope.launch {
                    wallpapers = firestoreManager.getWallpapers()
                    userRole = firestoreManager.getUserRole()
                    isPremiumUser = firestoreManager.isPremiumMember()
                    isLoading = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Listen for payment events (Premium Pass purchase)
    LaunchedEffect(Unit) {
        launch {
            PaymentBus.paymentSuccessFlow.collect { paymentId ->
                if (isPurchasingPremium || PaymentBus.pendingPaymentType == PaymentBus.TYPE_PREMIUM_PASS) {
                    isPurchasingPremium = false
                    PaymentBus.pendingPaymentType = null
                    val marked = firestoreManager.markUserAsPremium()
                    if (marked) {
                        isPremiumUser = true
                        Toast.makeText(
                            context,
                            "Premium Pass activated! All premium wallpapers unlocked and ads removed.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "Payment received. Error updating profile.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        launch {
            PaymentBus.paymentErrorFlow.collect { (code, response) ->
                if (isPurchasingPremium) {
                    isPurchasingPremium = false
                    PaymentBus.pendingPaymentType = null
                    Toast.makeText(context, "Payment failed or cancelled: $response", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchPremiumCheckout() {
        if (activity == null) {
            Toast.makeText(context, "Payment gateway unavailable", Toast.LENGTH_SHORT).show()
            return
        }
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fitwalls_logo),
                            contentDescription = "FitWalls Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("FitWalls", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (userRole == "creator") {
                        IconButton(onClick = onNavigateToUpload) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Wallpaper")
                        }
                    }
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Account Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToGenerator) {
                Icon(Icons.Default.Add, contentDescription = "Generate AI Wallpaper")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isPremiumUser) {
                        Card(
                            onClick = { launchPremiumCheckout() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.WorkspacePremium,
                                                contentDescription = "Premium Pass",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Unlock Premium — ₹${Constants.PREMIUM_PASS_PRICE_RUPEES}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "All premium wallpapers, no ads",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { launchPremiumCheckout() },
                                    enabled = !isPurchasingPremium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isPurchasingPremium) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            "Unlock",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        lazyItems(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    
                    if (filteredWallpapers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                "No wallpapers in this category yet.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredWallpapers) { wallpaper ->
                                WallpaperCard(wallpaper, onClick = { onNavigateToPreview(wallpaper.id) })
                            }
                        }
                    }
                    
                    if (!isPremiumUser) {
                        // REAL Banner Ad Unit ID: ca-app-pub-6916958158520465/1961203482
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-6916958158520465/1961203482"
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.imageUrl,
                contentDescription = wallpaper.title.ifBlank { wallpaper.prompt },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = wallpaper.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = wallpaper.creatorName.ifBlank { "Anonymous" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (wallpaper.pricingType) {
                                "Paid" -> "₹${wallpaper.price}"
                                "Premium-only" -> "Premium"
                                else -> "Free"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (wallpaper.pricingType) {
                                "Paid" -> MaterialTheme.colorScheme.primary
                                "Premium-only" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
            }
        }
    }
}

