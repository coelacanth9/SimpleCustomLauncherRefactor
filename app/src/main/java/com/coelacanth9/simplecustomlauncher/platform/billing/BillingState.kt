package com.coelacanth9.simplecustomlauncher.platform.billing

sealed class BillingConnectionState {
    object NotConnected : BillingConnectionState()
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    data class Error(val message: String, val responseCode: Int) : BillingConnectionState()
}

data class ProductInfo(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String
)

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Pending : PurchaseState()
    object Purchased : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
