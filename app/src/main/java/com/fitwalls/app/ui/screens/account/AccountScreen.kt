package com.fitwalls.app.ui.screens.account

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitwalls.app.auth.AuthManager
import com.fitwalls.app.data.CreatorEarnings
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToEditWallpaper: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val authManager = remember { AuthManager(context) }
    val firestoreManager = remember { FirestoreManager() }

    val user = auth.currentUser
    var role by remember { mutableStateOf<String?>("user") }
    var isLoading by remember { mutableStateOf(true) }

    // Creator data
    var earnings by remember { mutableStateOf(CreatorEarnings()) }
    var myWallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }

    // Payment details state
    var paymentMethod by remember { mutableStateOf("upi") } // "upi" or "bank"
    var upiId by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }
    var isSavingPayment by remember { mutableStateOf(false) }

    // Load user role, earnings, uploads, and payment details
    LaunchedEffect(Unit) {
        val r = firestoreManager.getUserRole() ?: "user"
        role = r
        if (r == "creator" && user != null) {
            earnings = firestoreManager.getCreatorEarnings(user.uid)
            myWallpapers = firestoreManager.getCreatorWallpapers(user.uid)

            val existingDetails = firestoreManager.getPaymentDetails()
            if (existingDetails != null) {
                paymentMethod = existingDetails["method"] as? String ?: "upi"
                upiId = existingDetails["upiId"] as? String ?: ""
                accountNumber = existingDetails["accountNumber"] as? String ?: ""
                ifscCode = existingDetails["ifsc"] as? String ?: ""
                accountHolderName = existingDetails["accountHolderName"] as? String ?: ""
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        authManager.signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Profile Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!user?.photoUrl?.toString().isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "User Avatar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.displayName?.ifBlank { "FitWalls User" } ?: "FitWalls User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user?.email ?: "No email available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = if (role == "creator") "CREATOR ACCOUNT" else "STANDARD USER",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (role == "creator") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = if (role == "creator") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Sign Out Button
                item {
                    OutlinedButton(
                        onClick = {
                            authManager.signOut()
                            onSignOut()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }

                if (role == "creator") {
                    // 1. Creator Earnings Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Creator Earnings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Sales (${earnings.totalSales} sold):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format("%.2f", earnings.grossTotal)}", fontWeight = FontWeight.Medium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Platform Fee (20%):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-₹${String.format("%.2f", earnings.platformCommission)}", color = MaterialTheme.colorScheme.error)
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Net Payable:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "₹${String.format("%.2f", earnings.netPayable)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    "Payouts are processed manually by the platform owner using the payment details below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. Creator Payment Details Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Payout Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    "Provide your payout destination for earnings withdrawal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Method toggle: UPI or Bank
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = paymentMethod == "upi",
                                        onClick = { paymentMethod = "upi" },
                                        label = { Text("UPI ID") },
                                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                    FilterChip(
                                        selected = paymentMethod == "bank",
                                        onClick = { paymentMethod = "bank" },
                                        label = { Text("Bank Transfer") },
                                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                }

                                if (paymentMethod == "upi") {
                                    OutlinedTextField(
                                        value = upiId,
                                        onValueChange = { upiId = it },
                                        label = { Text("UPI ID (e.g. name@okhdfcbank)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = accountHolderName,
                                        onValueChange = { accountHolderName = it },
                                        label = { Text("Account Holder Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = accountNumber,
                                        onValueChange = { accountNumber = it },
                                        label = { Text("Bank Account Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = ifscCode,
                                        onValueChange = { ifscCode = it },
                                        label = { Text("IFSC Code") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isSavingPayment = true
                                            // This is for the app owner to manually reference when paying out creator earnings — it does NOT process automatic split payments.
                                            // Automatic payment splitting would require a server-side integration (e.g. Firebase Cloud Functions with Razorpay Route API),
                                            // a separate future task, since it needs the Razorpay Key Secret which must never be stored in the Android app.
                                            val details = if (paymentMethod == "upi") {
                                                mapOf(
                                                    "method" to "upi",
                                                    "upiId" to upiId.trim()
                                                )
                                            } else {
                                                mapOf(
                                                    "method" to "bank",
                                                    "accountHolderName" to accountHolderName.trim(),
                                                    "accountNumber" to accountNumber.trim(),
                                                    "ifsc" to ifscCode.trim().uppercase()
                                                )
                                            }
                                            val success = firestoreManager.savePaymentDetails(details)
                                            isSavingPayment = false
                                            if (success) {
                                                Toast.makeText(context, "Payment details saved successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save payment details", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                ) {
                                    if (isSavingPayment) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text("Save Payment Details")
                                    }
                                }
                            }
                        }
                    }

                    // 3. My Uploads Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("My Uploads (${myWallpapers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = onNavigateToUpload) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload New")
                            }
                        }
                    }

                    if (myWallpapers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("You haven't uploaded any wallpapers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FilledTonalButton(onClick = onNavigateToUpload) {
                                        Text("Upload Wallpaper")
                                    }
                                }
                            }
                        }
                    } else {
                        // Grid of uploads
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                myWallpapers.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { wallpaper ->
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onNavigateToEditWallpaper(wallpaper.id) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Box(modifier = Modifier.height(200.dp)) {
                                                    AsyncImage(
                                                        model = wallpaper.imageUrl,
                                                        contentDescription = wallpaper.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text(
                                                                text = wallpaper.title.ifBlank { "Untitled" },
                                                                fontWeight = FontWeight.Bold,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                maxLines = 1
                                                            )
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = wallpaper.style,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = if (wallpaper.pricingType == "Paid") "₹${wallpaper.price}" else "Free",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            Text(
                                                                text = "Tap to Edit",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
