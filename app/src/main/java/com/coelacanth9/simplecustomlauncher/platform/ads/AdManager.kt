package com.coelacanth9.simplecustomlauncher.platform.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.coelacanth9.simplecustomlauncher.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AdState {
    data object NotLoaded : AdState()
    data object Loading : AdState()
    data object Ready : AdState()
    data object Showing : AdState()
    data class Error(val message: String) : AdState()
}

class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-2212870341311223/6138825796"
    }

    private var rewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    private var onRewardEarned: (() -> Unit)? = null

    fun initialize() {
        MobileAds.initialize(context) { loadRewardedAd() }
    }

    fun loadRewardedAd() {
        if (_adState.value == AdState.Loading) return
        _adState.value = AdState.Loading
        RewardedAd.load(context, getAdUnitId(), AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    _adState.value = AdState.Ready
                    setupFullScreenContentCallback()
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    _adState.value = AdState.Error(loadAdError.message)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd ?: run {
            _adState.value = AdState.Error("広告の準備ができていません")
            return
        }
        onRewardEarned = onRewarded
        _adState.value = AdState.Showing
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewardEarned?.invoke()
            onRewardEarned = null
        }
    }

    private fun setupFullScreenContentCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _adState.value = AdState.NotLoaded
                loadRewardedAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _adState.value = AdState.Error(adError.message)
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed")
            }
        }
    }

    private fun getAdUnitId(): String =
        if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    fun isAdReady(): Boolean = _adState.value == AdState.Ready
}
