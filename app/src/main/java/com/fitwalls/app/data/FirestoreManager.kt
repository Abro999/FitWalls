package com.fitwalls.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Wallpaper(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val imageUrl: String = "",
    val prompt: String = "",
    val style: String = "",
    val pricingType: String = "Free"
)

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun saveGeneratedWallpaper(imageUrl: String, prompt: String, style: String): Boolean {
        val user = auth.currentUser ?: return false
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
    
    suspend fun getWallpapers(): List<Wallpaper> {
        return try {
            val snapshot = db.collection("wallpapers").get().await()
            snapshot.toObjects(Wallpaper::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
