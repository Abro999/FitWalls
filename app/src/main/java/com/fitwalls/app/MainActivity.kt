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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Checkout.preload(applicationContext)
        
        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Register test devices so real ads don't get accidentally clicked/counted during development
        // TODO: Grab your device's Test Device ID from Logcat on first run
        // (Search for "Use RequestConfiguration.Builder().setTestDeviceIds" in Logcat) and add it to the list below.
        val testDeviceIds = listOf(
            AdRequest.DEVICE_ID_EMULATOR
            // TODO: Add your physical test device hashed ID here, e.g. "B3EEABB8EE11C2BE770B684D95219ECB"
        )
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

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
