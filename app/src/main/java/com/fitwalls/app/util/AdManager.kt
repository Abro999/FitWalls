package com.fitwalls.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdManager manages AdMob ad unit IDs, interstitial ad loading,
 * and the in-memory wallpaper apply counter.
 *
 * AdMob ID Classification:
 * - REAL App ID: ca-app-pub-6916958158520465~4343824240 (configured in AndroidManifest.xml)
 * - REAL Banner Ad ID: ca-app-pub-6916958158520465/1961203482 (used in HomeScreen.kt)
 * - REAL Rewarded Ad ID: ca-app-pub-6916958158520465/3246364999 (used in GeneratorScreen.kt)
 * - TEST Interstitial Ad ID: ca-app-pub-3940256099942544/1033173712 (used below)
 */
object AdManager {
    private const val TAG = "AdManager"

    // TEST Interstitial Ad Unit ID for now
    // TODO: Replace with your REAL Interstitial Ad Unit ID once created in your AdMob dashboard
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Simple in-memory counter tracking successful wallpaper applies
    var applyCount: Int = 0
        private set

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading: Boolean = false

    /**
     * Preloads an interstitial ad if not already loaded or loading.
     */
    fun loadInterstitialAd(context: Context, isPremiumUser: Boolean = false) {
        if (isPremiumUser || interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Call when a wallpaper is successfully applied.
     * Increments the apply counter and displays an interstitial ad on every 3rd successful apply.
     */
    fun onWallpaperApplied(activity: Activity?, isPremiumUser: Boolean = false) {
        applyCount++
        Log.d(TAG, "Wallpaper applied count: $applyCount")

        if (applyCount % 3 == 0 && !isPremiumUser && activity != null) {
            if (interstitialAd != null) {
                interstitialAd?.show(activity)
                interstitialAd = null
                // Reload ad for the next 3rd cycle
                loadInterstitialAd(activity, isPremiumUser)
            } else {
                // If ad wasn't loaded in time, attempt loading for next opportunity
                loadInterstitialAd(activity, isPremiumUser)
            }
        }
    }
}
