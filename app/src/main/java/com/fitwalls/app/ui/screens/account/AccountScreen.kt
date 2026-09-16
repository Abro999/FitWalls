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
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
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
    var isPremiumMember by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Creator data
    var earnings by remember { mutableStateOf(CreatorEarnings()) }
    var myWallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }

    // Payment details state (Razorpay Route)
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isSavingPayment by remember { mutableStateOf(false) }
    var paymentErrorMessage by remember { mutableStateOf<String?>(null) }
    var submissionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var razorpayLinkedAccountId by remember { mutableStateOf<String?>(null) }
    var isVerificationPending by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }

    // Load user role, earnings, uploads, and payment details
    LaunchedEffect(Unit) {
        val r = firestoreManager.getUserRole() ?: "user"
        role = r
        isPremiumMember = firestoreManager.isPremiumMember()
        if (r == "creator" && user != null) {
            earnings = firestoreManager.getCreatorEarnings(user.uid)
            myWallpapers = firestoreManager.getCreatorWallpapers(user.uid)

            val profileData = firestoreManager.getCreatorProfileData()
            if (profileData != null) {
                val linkedId = profileData["razorpayLinkedAccountId"] as? String
                    ?: profileData["linkedAccountId"] as? String
                    ?: profileData["accountId"] as? String
                val pStatus = (profileData["payoutStatus"] as? String
                    ?: profileData["accountStatus"] as? String
                    ?: profileData["verificationStatus"] as? String)?.lowercase()
                
                @Suppress("UNCHECKED_CAST")
                val paymentDetails = profileData["paymentDetails"] as? Map<String, Any>
                
                accountNumber = paymentDetails?.get("bankAccountNumber") as? String
                    ?: paymentDetails?.get("accountNumber") as? String
                    ?: profileData["bankAccountNumber"] as? String
                    ?: profileData["accountNumber"] as? String ?: ""
                ifscCode = paymentDetails?.get("ifsc") as? String
                    ?: profileData["ifsc"] as? String ?: ""
                accountHolderName = paymentDetails?.get("accountHolderName") as? String
                    ?: profileData["accountHolderName"] as? String
                    ?: user.displayName ?: ""
                phoneNumber = paymentDetails?.get("phone") as? String
                    ?: profileData["phone"] as? String
                    ?: user.phoneNumber ?: ""

                razorpayLinkedAccountId = linkedId
                
                if (pStatus == "verified" || pStatus == "active") {
                    isVerified = true
                    isVerificationPending = false
                } else if (pStatus == "pending" || !linkedId.isNullOrBlank() || paymentDetails != null) {
                    isVerificationPending = true
                    isVerified = false
                }
            } else {
                accountHolderName = user.displayName ?: ""
                phoneNumber = user.phoneNumber ?: ""
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
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = {
                                            Text(
                                                text = if (role == "creator") "CREATOR" else "STANDARD",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (role == "creator") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = if (role == "creator") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                    if (isPremiumMember) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = {
                                                Text(
                                                    text = "PREMIUM PASS",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        )
                                    }
                                }
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
                                    "Creator split payouts are processed automatically via Razorpay Route upon verified purchases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. Creator Payment Details Section (Razorpay Route)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Payment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }

                                    // Verification Status Badge
                                    if (isVerified) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Verified", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                iconContentColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    } else if (isVerificationPending) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Verification pending", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                            icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                                iconContentColor = MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    } else {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Not Linked", fontSize = 12.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Text(
                                    "Enter your bank account details for Razorpay Route automatic creator payouts. Once verified, sales are split directly to your bank account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!razorpayLinkedAccountId.isNullOrBlank()) {
                                    Text(
                                        "Linked Account: $razorpayLinkedAccountId",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Success Notification Banner
                                if (submissionSuccessMessage != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = submissionSuccessMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Error Notification Banner
                                if (paymentErrorMessage != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Submission Error",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = paymentErrorMessage ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = accountHolderName,
                                    onValueChange = {
                                        accountHolderName = it
                                        paymentErrorMessage = null
                                    },
                                    label = { Text("Account Holder Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                )

                                OutlinedTextField(
                                    value = accountNumber,
                                    onValueChange = {
                                        accountNumber = it
                                        paymentErrorMessage = null
                                    },
                                    label = { Text("Bank Account Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                )

                                OutlinedTextField(
                                    value = ifscCode,
                                    onValueChange = {
                                        ifscCode = it.uppercase()
                                        paymentErrorMessage = null
                                    },
                                    label = { Text("IFSC Code (e.g. HDFC0001234)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                )

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = {
                                        phoneNumber = it
                                        paymentErrorMessage = null
                                    },
                                    label = { Text("Phone Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                )

                                // Email info display (from signed-in user)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Account Email:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user?.email ?: "No email", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }

                                Button(
                                    onClick = {
                                        if (accountHolderName.isBlank()) {
                                            paymentErrorMessage = "Please enter the account holder name."
                                            return@Button
                                        }
                                        if (accountNumber.isBlank() || accountNumber.length < 5) {
                                            paymentErrorMessage = "Please enter a valid bank account number."
                                            return@Button
                                        }
                                        if (ifscCode.isBlank() || ifscCode.length < 4) {
                                            paymentErrorMessage = "Please enter a valid bank IFSC code."
                                            return@Button
                                        }

                                        scope.launch {
                                            isSavingPayment = true
                                            paymentErrorMessage = null
                                            submissionSuccessMessage = null

                                            try {
                                                val functions = FirebaseFunctions.getInstance()
                                                val email = user?.email?.ifBlank { "creator@fitwalls.app" } ?: "creator@fitwalls.app"
                                                val phone = phoneNumber.trim().ifBlank { user?.phoneNumber ?: "9999999999" }

                                                val requestData = hashMapOf(
                                                    "accountHolderName" to accountHolderName.trim(),
                                                    "bankAccountNumber" to accountNumber.trim(),
                                                    "ifsc" to ifscCode.trim().uppercase(),
                                                    "email" to email,
                                                    "phone" to phone
                                                )

                                                val result = functions.getHttpsCallable("createCreatorLinkedAccount")
                                                    .call(requestData)
                                                    .await()

                                                val resultData = result.data as? Map<*, *>
                                                val returnedAccountId = (resultData?.get("accountId")
                                                    ?: resultData?.get("account_id")
                                                    ?: resultData?.get("razorpayLinkedAccountId")
                                                    ?: resultData?.get("id")) as? String
                                                val returnedStatus = (resultData?.get("status") as? String)?.lowercase()

                                                if (!returnedAccountId.isNullOrBlank()) {
                                                    razorpayLinkedAccountId = returnedAccountId
                                                }

                                                if (returnedStatus == "verified" || returnedStatus == "active") {
                                                    isVerified = true
                                                    isVerificationPending = false
                                                } else {
                                                    isVerificationPending = true
                                                    isVerified = false
                                                }

                                                firestoreManager.updateCreatorPayoutStatus(
                                                    linkedAccountId = returnedAccountId ?: razorpayLinkedAccountId,
                                                    payoutStatus = if (isVerified) "verified" else "pending",
                                                    details = mapOf(
                                                        "accountHolderName" to accountHolderName.trim(),
                                                        "accountNumber" to accountNumber.trim(),
                                                        "bankAccountNumber" to accountNumber.trim(),
                                                        "ifsc" to ifscCode.trim().uppercase(),
                                                        "phone" to phone,
                                                        "email" to email
                                                    )
                                                )

                                                submissionSuccessMessage = "Payment details submitted for verification"
                                                Toast.makeText(context, "Payment details submitted for verification", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                paymentErrorMessage = e.localizedMessage ?: e.message ?: "Failed to submit payment details. Please check your bank information and retry."
                                                Toast.makeText(context, "Submission failed: $paymentErrorMessage", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSavingPayment = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPayment
                                ) {
                                    if (isSavingPayment) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submitting to Razorpay Route...")
                                    } else {
                                        Text(if (isVerified || isVerificationPending) "Update Payment Details" else "Save Payment Details")
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
