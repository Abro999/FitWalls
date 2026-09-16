package com.fitwalls.app.ui.screens.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitwalls.app.data.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TERMS_URL = "https://abro999.github.io/fitwalls-website/#terms"
private const val PRIVACY_URL = "https://abro999.github.io/fitwalls-website/#privacy"

enum class RoleSelectionStep {
    SELECT_ROLE_AND_TERMS,
    CREATOR_BANK_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(onRoleSelected: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val firestoreManager = remember { FirestoreManager() }
    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser

    var currentStep by remember { mutableStateOf(RoleSelectionStep.SELECT_ROLE_AND_TERMS) }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var roleError by remember { mutableStateOf<String?>(null) }

    // Creator onboarding form states
    var accountHolderName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var isSubmittingCreator by remember { mutableStateOf(false) }
    var creatorSuccessMessage by remember { mutableStateOf<String?>(null) }
    var creatorErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentStep == RoleSelectionStep.SELECT_ROLE_AND_TERMS) {
                            "Choose Account Type"
                        } else {
                            "Creator Onboarding"
                        }
                    )
                },
                navigationIcon = {
                    if (currentStep == RoleSelectionStep.CREATOR_BANK_DETAILS && !isSubmittingCreator) {
                        IconButton(onClick = {
                            currentStep = RoleSelectionStep.SELECT_ROLE_AND_TERMS
                            creatorErrorMessage = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (currentStep == RoleSelectionStep.SELECT_ROLE_AND_TERMS) {
            // STEP 1: Role Selection & Mandatory Terms Acceptance
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "How will you use FitWalls?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Select an account type and accept our terms to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                RoleCard(
                    title = "I'm a User",
                    description = "Browse, preview, and apply amazing AI wallpapers to your device.",
                    isSelected = selectedRole == "user",
                    onClick = { selectedRole = "user" }
                )

                RoleCard(
                    title = "I'm a Creator",
                    description = "Upload your designs, earn revenue from premium sales, and manage payouts.",
                    isSelected = selectedRole == "creator",
                    onClick = { selectedRole = "creator" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mandatory Terms & Privacy Checkbox with Clickable Links
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val termsAnnotatedString = remember {
                            buildAnnotatedString {
                                append("I agree to the ")
                                pushStringAnnotation(tag = "URL", annotation = TERMS_URL)
                                withStyle(
                                    style = SpanStyle(
                                        color = androidx.compose.ui.graphics.Color(0xFF1976D2),
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Terms of Use")
                                }
                                pop()
                                append(" and ")
                                pushStringAnnotation(tag = "URL", annotation = PRIVACY_URL)
                                withStyle(
                                    style = SpanStyle(
                                        color = androidx.compose.ui.graphics.Color(0xFF1976D2),
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Privacy Policy")
                                }
                                pop()
                            }
                        }

                        ClickableText(
                            text = termsAnnotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { offset ->
                                termsAnnotatedString.getStringAnnotations(
                                    tag = "URL",
                                    start = offset,
                                    end = offset
                                ).firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (e: Exception) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }

                if (roleError != null) {
                    Text(
                        text = roleError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                Button(
                    onClick = {
                        val role = selectedRole ?: return@Button
                        if (!acceptedTerms) return@Button

                        if (role == "creator") {
                            // Proceed directly to creator bank onboarding step
                            currentStep = RoleSelectionStep.CREATOR_BANK_DETAILS
                        } else {
                            // User role: save profile with acceptedTermsAt and go directly to HomeRoute
                            isSaving = true
                            roleError = null
                            scope.launch {
                                val success = firestoreManager.createUserProfile("user")
                                if (success) {
                                    onRoleSelected()
                                } else {
                                    roleError = "Failed to save profile. Please try again."
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = selectedRole != null && acceptedTerms && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (selectedRole == "creator") "Continue to Creator Setup" else "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // STEP 2: Creator Onboarding — Bank Details Form & Verified Google Email
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Creator Payout Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Provide your bank details to receive earnings from wallpaper purchases. We link this directly to your verified Google account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Read-only Verified Google Email Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified Google Account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Verified Google Account",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Verified email: ${user?.email ?: "account@gmail.com"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Success Message Banner
                if (creatorSuccessMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = creatorSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Error Message Banner
                if (creatorErrorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Submission Error",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = creatorErrorMessage ?: "",
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
                        creatorErrorMessage = null
                    },
                    label = { Text("Account Holder Name *") },
                    placeholder = { Text("As shown on bank records") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmittingCreator
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = {
                        accountNumber = it
                        creatorErrorMessage = null
                    },
                    label = { Text("Bank Account Number *") },
                    placeholder = { Text("e.g. 0123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmittingCreator
                )

                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = {
                        ifscCode = it.uppercase()
                        creatorErrorMessage = null
                    },
                    label = { Text("IFSC Code *") },
                    placeholder = { Text("e.g. HDFC0001234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmittingCreator
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        creatorErrorMessage = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("10-digit mobile number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmittingCreator
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (accountHolderName.isBlank()) {
                            creatorErrorMessage = "Please enter the account holder name."
                            return@Button
                        }
                        if (accountNumber.isBlank() || accountNumber.length < 5) {
                            creatorErrorMessage = "Please enter a valid bank account number."
                            return@Button
                        }
                        if (ifscCode.isBlank() || ifscCode.length < 4) {
                            creatorErrorMessage = "Please enter a valid IFSC code (e.g. HDFC0001234)."
                            return@Button
                        }

                        scope.launch {
                            isSubmittingCreator = true
                            creatorErrorMessage = null
                            creatorSuccessMessage = null

                            try {
                                // 1. Save creator user profile with accepted terms in Firestore
                                firestoreManager.createUserProfile("creator")

                                // 2. Call Cloud Function "createCreatorLinkedAccount"
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
                                val isVerified = (returnedStatus == "verified" || returnedStatus == "active")

                                // 3. Store payout status in Firestore
                                firestoreManager.updateCreatorPayoutStatus(
                                    linkedAccountId = returnedAccountId,
                                    payoutStatus = if (isVerified) "verified" else "pending",
                                    details = mapOf(
                                        "accountHolderName" to accountHolderName.trim(),
                                        "bankAccountNumber" to accountNumber.trim(),
                                        "accountNumber" to accountNumber.trim(),
                                        "ifsc" to ifscCode.trim().uppercase(),
                                        "phone" to phone,
                                        "email" to email
                                    )
                                )

                                val successText = "Creator account created — payment details submitted for verification"
                                creatorSuccessMessage = successText
                                Toast.makeText(context, successText, Toast.LENGTH_LONG).show()

                                // Small delay so the user clearly sees confirmation, then navigate to HomeRoute
                                delay(1200)
                                onRoleSelected()
                            } catch (e: Exception) {
                                val msg = e.localizedMessage ?: e.message ?: "Failed to submit payment details."
                                creatorErrorMessage = msg
                                Toast.makeText(context, "Submission error: $msg", Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmittingCreator = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isSubmittingCreator &&
                            accountHolderName.isNotBlank() &&
                            accountNumber.isNotBlank() &&
                            ifscCode.isNotBlank()
                ) {
                    if (isSubmittingCreator) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Submitting Payment Details...")
                    } else {
                        Text(
                            "Complete Creator Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
