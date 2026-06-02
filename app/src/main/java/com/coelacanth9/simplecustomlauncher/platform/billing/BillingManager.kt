package com.coelacanth9.simplecustomlauncher.platform.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.coelacanth9.simplecustomlauncher.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val onPurchaseComplete: () -> Unit,
    private val onPurchaseCleared: () -> Unit = {}
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "Billing"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: ProductDetails? = null

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.NotConnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productInfo = MutableStateFlow<ProductInfo?>(null)
    val productInfo: StateFlow<ProductInfo?> = _productInfo.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _isPurchased = MutableStateFlow(false)
    val isPurchased: StateFlow<Boolean> = _isPurchased.asStateFlow()

    fun initialize() {
        Log.d(TAG, "initialize() Ver: ${BuildConfig.VERSION_NAME}")
        if (billingClient != null) return
        _connectionState.value = BillingConnectionState.Connecting
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: code=${result.responseCode}")
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    queryProductDetails()
                    restorePurchases()
                } else {
                    _connectionState.value = BillingConnectionState.Error(result.debugMessage, result.responseCode)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected: retrying in ${RECONNECT_DELAY_MS}ms")
                _connectionState.value = BillingConnectionState.NotConnected
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    startConnection()
                }
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        billingClient?.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        ) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull()
                val offerDetails = details?.oneTimePurchaseOfferDetails
                if (details != null && offerDetails != null) {
                    cachedProductDetails = details
                    _productInfo.value = ProductInfo(
                        productId = details.productId,
                        title = details.title,
                        description = details.description,
                        formattedPrice = offerDetails.formattedPrice,
                        priceAmountMicros = offerDetails.priceAmountMicros,
                        priceCurrencyCode = offerDetails.priceCurrencyCode
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        if (billingClient?.isReady != true) return
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchasesList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchasesList.find {
                    it.products.contains(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (premiumPurchase != null) {
                    _isPurchased.value = true
                    onPurchaseComplete()
                    if (!premiumPurchase.isAcknowledged) acknowledgePurchase(premiumPurchase)
                } else {
                    _isPurchased.value = false
                    onPurchaseCleared()
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = cachedProductDetails ?: run {
            _purchaseState.value = PurchaseState.Error("商品情報を読み込めませんでした")
            return
        }
        val client = billingClient ?: run {
            _purchaseState.value = PurchaseState.Error("課金サービスに接続できません")
            return
        }
        if (!client.isReady) {
            _purchaseState.value = PurchaseState.Error("課金サービスに接続できません")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
            ))
            .build()
        _purchaseState.value = PurchaseState.Pending
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error("購入を開始できませんでした")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _purchaseState.value = PurchaseState.Idle
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> _purchaseState.value = PurchaseState.Error("購入に失敗しました")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                _isPurchased.value = true
                onPurchaseComplete()
                acknowledgePurchase(purchase)
            } else {
                completePurchase()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _purchaseState.value = PurchaseState.Pending
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        billingClient?.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) { result ->
            Log.d(TAG, "acknowledgePurchase: code=${result.responseCode}")
            _purchaseState.value = PurchaseState.Purchased
        }
    }

    private fun completePurchase() {
        _isPurchased.value = true
        _purchaseState.value = PurchaseState.Purchased
        onPurchaseComplete()
    }

    fun endConnection() {
        scope.cancel()
        billingClient?.endConnection()
        billingClient = null
        cachedProductDetails = null
        _connectionState.value = BillingConnectionState.NotConnected
    }
}
