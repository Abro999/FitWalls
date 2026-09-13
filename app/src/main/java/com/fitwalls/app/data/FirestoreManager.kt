package com.fitwalls.app.data

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class Wallpaper(
    @DocumentId var id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val prompt: String = "",
    val style: String = "",
    val pricingType: String = "Free",
    val price: Double = 0.0
)

data class CreatorEarnings(
    val grossTotal: Double = 0.0,
    val platformCommission: Double = 0.0,
    val netPayable: Double = 0.0,
    val totalSales: Int = 0
)

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    suspend fun getUserRole(): String? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = db.collection("users").document(user.uid).get().await()
            snapshot.getString("role")
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createUserProfile(role: String): Boolean {
        val user = auth.currentUser ?: return false
        val userProfile = hashMapOf(
            "uid" to user.uid,
            "displayName" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "role" to role
        )
        return try {
            db.collection("users").document(user.uid).set(userProfile).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun uploadImageBytesToStorage(imageBytes: ByteArray): String? {
        val user = auth.currentUser ?: return null
        return try {
            val path = "wallpapers/${user.uid}/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(path)
            
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun uploadImageToStorage(base64Image: String): String? {
        return try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            uploadImageBytesToStorage(imageBytes)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun uploadCreatorWallpaper(imageBytes: ByteArray, title: String, category: String, pricingType: String, price: Double): Boolean {
        val user = auth.currentUser ?: return false
        
        val imageUrl = uploadImageBytesToStorage(imageBytes) ?: return false
        
        val wallpaper = Wallpaper(
            creatorId = user.uid,
            creatorName = user.displayName ?: "Anonymous",
            imageUrl = imageUrl,
            title = title,
            style = category,
            pricingType = pricingType,
            price = price
        )
        
        return try {
            db.collection("wallpapers").add(wallpaper).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun saveGeneratedWallpaper(base64Image: String, prompt: String, style: String): Boolean {
        val user = auth.currentUser ?: return false
        
        val imageUrl = uploadImageToStorage(base64Image) ?: return false
        
        val wallpaper = Wallpaper(
            creatorId = user.uid,
            creatorName = user.displayName ?: "Anonymous",
            imageUrl = imageUrl,
            prompt = prompt,
            style = style,
            pricingType = "Free"
        )
        
        return try {
            db.collection("wallpapers").add(wallpaper).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Opted for fetching all and doing client-side filtering in HomeScreen.
    // While server-side filtering via whereEqualTo("style", category) is possible,
    // client-side filtering prevents a new Firestore read query every time the user 
    // taps a different category chip, saving on read costs and improving UI responsiveness.
    suspend fun getWallpapers(): List<Wallpaper> {
        return try {
            val snapshot = db.collection("wallpapers").get().await()
            snapshot.toObjects(Wallpaper::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWallpaper(id: String): Wallpaper? {
        return try {
            val snapshot = db.collection("wallpapers").document(id).get().await()
            snapshot.toObject(Wallpaper::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasUserPurchased(wallpaperId: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val snapshot = db.collection("purchases")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("wallpaperId", wallpaperId)
                .get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recordPurchase(wallpaperId: String, creatorId: String, pricePaid: Double, razorpayPaymentId: String): Boolean {
        val user = auth.currentUser ?: return false
        val purchase = hashMapOf(
            "userId" to user.uid,
            "wallpaperId" to wallpaperId,
            "creatorId" to creatorId,
            "pricePaid" to pricePaid,
            "razorpayPaymentId" to razorpayPaymentId,
            "purchasedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        return try {
            db.collection("purchases").add(purchase).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCreatorWallpapers(creatorId: String): List<Wallpaper> {
        return try {
            val snapshot = db.collection("wallpapers")
                .whereEqualTo("creatorId", creatorId)
                .get().await()
            snapshot.toObjects(Wallpaper::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCreatorEarnings(creatorId: String): CreatorEarnings {
        return try {
            val snapshot = db.collection("purchases")
                .whereEqualTo("creatorId", creatorId)
                .get().await()
            var gross = 0.0
            var count = 0
            for (doc in snapshot.documents) {
                val price = doc.getDouble("pricePaid") ?: 0.0
                gross += price
                count++
            }
            val commission = gross * 0.20 // 20% platform commission
            val net = gross - commission
            CreatorEarnings(
                grossTotal = gross,
                platformCommission = commission,
                netPayable = net,
                totalSales = count
            )
        } catch (e: Exception) {
            CreatorEarnings()
        }
    }

    suspend fun updateWallpaper(wallpaperId: String, updatedFields: Map<String, Any>): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val docRef = db.collection("wallpapers").document(wallpaperId)
            val snapshot = docRef.get().await()
            if (snapshot.getString("creatorId") != user.uid) {
                return false // unauthorized
            }
            docRef.update(updatedFields).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteWallpaper(wallpaperId: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val docRef = db.collection("wallpapers").document(wallpaperId)
            val snapshot = docRef.get().await()
            if (snapshot.getString("creatorId") != user.uid) {
                return false // unauthorized
            }
            val imageUrl = snapshot.getString("imageUrl")
            docRef.delete().await()

            if (!imageUrl.isNullOrBlank()) {
                try {
                    storage.getReferenceFromUrl(imageUrl).delete().await()
                } catch (ignored: Exception) {
                    // Ignore storage deletion errors
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // This is for the app owner to manually reference when paying out creator earnings.
    // It does NOT process automatic split payments.
    // NOTE: Automatic payment splitting would require a server-side integration
    // (e.g. Firebase Cloud Functions with Razorpay Route API), a separate future task,
    // since it needs the Razorpay Key Secret which must never be stored in the Android app.
    suspend fun savePaymentDetails(paymentDetails: Map<String, Any>): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            db.collection("users").document(user.uid)
                .set(mapOf("paymentDetails" to paymentDetails), com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPaymentDetails(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = db.collection("users").document(user.uid).get().await()
            @Suppress("UNCHECKED_CAST")
            snapshot.get("paymentDetails") as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
    }
}
