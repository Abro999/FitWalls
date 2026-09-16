package com.fitwalls.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitwalls.app.ui.navigation.AppNavigation
import com.fitwalls.app.ui.theme.FitWallsTheme
import com.fitwalls.app.util.PaymentBus
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import android.util.Log
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Explicitly initialize Firebase first to ensure all Firebase services work reliably
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp initialization failed", e)
        }

        // 2. Preload Razorpay Checkout safely
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Razorpay Checkout preload failed", e)
        }
        
        // 3. Initialize the Google Mobile Ads SDK safely
        try {
            MobileAds.initialize(this) {}
            val testDeviceIds = listOf(
                AdRequest.DEVICE_ID_EMULATOR
            )
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        } catch (e: Exception) {
            Log.e("MainActivity", "MobileAds initialization failed", e)
        }

        enableEdgeToEdge()
        setContent {
            FitWallsTheme {
                AppNavigation()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            PaymentBus.onSuccess(razorpayPaymentId)
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            PaymentBus.onError(code, response)
        }
    }
}
