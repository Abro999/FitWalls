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
}
