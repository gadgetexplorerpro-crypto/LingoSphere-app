package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.model.SubscriptionPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class BillingPurchaseStatus {
  IDLE,
  CONNECTING,
  LAUNCHING_FLOW,
  PURCHASED,
  VERIFIED_ACTIVE,
  PENDING,
  USER_CANCELED,
  RESTORED,
  NO_ACTIVE_PURCHASES_FOUND,
  EXPIRED,
  FAILED
}

enum class BillingAvailability {
  CONNECTING,
  READY,
  UNAVAILABLE_PREVIEW
}

data class PlanPricingInfo(
  val productId: String,
  val basePlanId: String? = null,
  val formattedPrice: String,
  val recurringPriceRaw: String,
  val priceAmountMicros: Long = 0L,
  val priceCurrencyCode: String = "USD",
  val equivalentMonthlyFormatted: String? = null,
  val savingsPercent: Int = 0,
  val billingPeriodIso: String,
  val billingFrequencyLabel: String,
  val billingCycleSuffix: String,
  val hasFreeTrial: Boolean = false,
  val trialDurationDays: Int = 0,
  val trialPeriodLabel: String? = null,
  val firstBillingDateFormatted: String = "",
  val billingPeriodText: String = "Billed ${billingFrequencyLabel.lowercase(Locale.US)} at $recurringPriceRaw$billingCycleSuffix • Cancel anytime",
  val offerToken: String? = null,
  val baseOfferToken: String? = null,
  val isLiveFromPlayStore: Boolean = false
)

data class VerifiedSubscriptionDetails(
  val tier: String,
  val purchaseToken: String,
  val productId: String,
  val basePlanId: String = "",
  val isFreeTrial: Boolean,
  val isAutoRenewing: Boolean,
  val purchaseTimeEpochMs: Long,
  val expiryEpochMs: Long
)

fun computeFirstBillingDateString(trialDays: Int = 7, baseEpochMs: Long = System.currentTimeMillis()): String {
  val cal = java.util.Calendar.getInstance().apply {
    timeInMillis = baseEpochMs
    add(java.util.Calendar.DAY_OF_YEAR, trialDays.coerceAtLeast(1))
  }
  return SimpleDateFormat("MMM d, yyyy", Locale.US).format(cal.time)
}

fun formatEpochDateReadable(epochMs: Long): String {
  if (epochMs <= 0L) return ""
  return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMs))
}

internal fun parseIsoPeriodToDays(isoPeriod: String?): Int {
  if (isoPeriod.isNullOrBlank()) return 7
  val upper = isoPeriod.trim().uppercase(Locale.US)
  val daysMatch = Regex("""(\d+)D""").find(upper)?.groupValues?.getOrNull(1)?.toIntOrNull()
  if (daysMatch != null && daysMatch > 0) return daysMatch
  val weeksMatch = Regex("""(\d+)W""").find(upper)?.groupValues?.getOrNull(1)?.toIntOrNull()
  if (weeksMatch != null && weeksMatch > 0) return weeksMatch * 7
  val monthsMatch = Regex("""(\d+)M""").find(upper)?.groupValues?.getOrNull(1)?.toIntOrNull()
  if (monthsMatch != null && monthsMatch > 0) return monthsMatch * 30
  return 7
}

internal fun formatMonthlyEquivalentFromMicros(yearlyMicros: Long, currencyCode: String): String {
  if (yearlyMicros <= 0L) return "$6.67/mo"
  val perMonthAmount = (yearlyMicros / 1_000_000.0) / 12.0
  return try {
    val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
    if (currencyCode.isNotBlank()) {
      fmt.currency = Currency.getInstance(currencyCode)
    }
    "${fmt.format(perMonthAmount)}/month"
  } catch (_: Exception) {
    "$${String.format(Locale.US, "%.2f", perMonthAmount)}/month"
  }
}

internal fun calculateYearlySavingsPercent(
  monthlyMicros: Long,
  yearlyMicros: Long,
  monthlyCurrency: String,
  yearlyCurrency: String
): Int {
  if (monthlyMicros <= 0L || yearlyMicros <= 0L) return 33
  if (!monthlyCurrency.equals(yearlyCurrency, ignoreCase = true)) return 33
  val twelveMonthMicros = monthlyMicros.toDouble() * 12.0
  if (twelveMonthMicros <= 0.0 || yearlyMicros >= twelveMonthMicros) return 0
  return (((twelveMonthMicros - yearlyMicros.toDouble()) / twelveMonthMicros) * 100.0)
    .roundToInt()
    .coerceIn(0, 99)
}

class BillingRepository(
  private val context: Context,
  private val externalScope: CoroutineScope,
  private val onEntitlementVerified: (tier: String, purchaseToken: String) -> Unit,
  private val onSubscriptionDetailsVerified: ((VerifiedSubscriptionDetails) -> Unit)? = null,
  private val onEntitlementRevoked: (() -> Unit)? = null
) {

  private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
  val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

  private val _planPricingMap = MutableStateFlow<Map<String, PlanPricingInfo>>(defaultCatalogPricing(hasUsedFreeTrial = false))
  val planPricingMap: StateFlow<Map<String, PlanPricingInfo>> = _planPricingMap.asStateFlow()

  private val _isBillingReady = MutableStateFlow(false)
  val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

  private val _billingAvailability = MutableStateFlow(BillingAvailability.CONNECTING)
  val billingAvailability: StateFlow<BillingAvailability> = _billingAvailability.asStateFlow()

  private val _purchaseStatus = MutableStateFlow(BillingPurchaseStatus.IDLE)
  val purchaseStatus: StateFlow<BillingPurchaseStatus> = _purchaseStatus.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _billingError = MutableStateFlow<String?>(null)
  val billingError: StateFlow<String?> = _billingError.asStateFlow()

  private val _isAutoRenewing = MutableStateFlow(true)
  val isAutoRenewing: StateFlow<Boolean> = _isAutoRenewing.asStateFlow()

  private val _verifiedPurchaseTimeMs = MutableStateFlow<Long?>(null)
  val verifiedPurchaseTimeMs: StateFlow<Long?> = _verifiedPurchaseTimeMs.asStateFlow()

  @Volatile
  private var activePurchaseToken: String? = null

  @Volatile
  private var activeProductId: String? = null

  @Volatile
  private var userHasUsedFreeTrial: Boolean = false

  @Volatile
  private var pendingIsFreeTrialFlow: Boolean = false

  @Volatile
  private var pendingRequestedBasePlanId: String = ""

  private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    when (billingResult.responseCode) {
      BillingClient.BillingResponseCode.OK -> {
        _billingError.value = null
        if (!purchases.isNullOrEmpty()) {
          for (purchase in purchases) {
            handlePurchase(purchase, isRestore = false, onRestoreCallback = null)
          }
        } else {
          _purchaseStatus.value = BillingPurchaseStatus.IDLE
        }
      }
      BillingClient.BillingResponseCode.USER_CANCELED -> {
        Log.i("BillingRepository", "User canceled Google Play subscription purchase flow")
        _purchaseStatus.value = BillingPurchaseStatus.USER_CANCELED
        _statusMessage.value = "Purchase canceled. You were not charged."
      }
      BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
        Log.i("BillingRepository", "Subscription already owned — restoring active entitlement")
        queryExistingPurchases(isExplicitRestore = true)
      }
      else -> {
        Log.w(
          "BillingRepository",
          "Billing error [${billingResult.responseCode}]: ${billingResult.debugMessage}"
        )
        _purchaseStatus.value = BillingPurchaseStatus.FAILED
        val errText = billingResult.debugMessage.ifBlank {
          "Unable to complete Google Play purchase (code ${billingResult.responseCode})."
        }
        _billingError.value = errText
        _statusMessage.value = errText
      }
    }
  }

  private val billingClient: BillingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases(
      PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .enablePrepaidPlans()
        .build()
    )
    .build()

  init {
    startBillingConnection()
  }

  fun getPlanPricingInfo(productId: String): PlanPricingInfo? {
    return _planPricingMap.value[productId]
      ?: defaultCatalogPricing(hasUsedFreeTrial = userHasUsedFreeTrial)[productId]
  }

  fun syncUserTrialEligibility(hasUsedTrial: Boolean) {
    if (userHasUsedFreeTrial != hasUsedTrial) {
      userHasUsedFreeTrial = hasUsedTrial
      rebuildPricingMapFromLoadedProducts()
    }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
    _billingError.value = null
    if (_purchaseStatus.value == BillingPurchaseStatus.USER_CANCELED ||
      _purchaseStatus.value == BillingPurchaseStatus.FAILED ||
      _purchaseStatus.value == BillingPurchaseStatus.NO_ACTIVE_PURCHASES_FOUND
    ) {
      _purchaseStatus.value = BillingPurchaseStatus.IDLE
    }
  }

  fun startBillingConnection() {
    if (billingClient.isReady) {
      _isBillingReady.value = true
      _billingAvailability.value = BillingAvailability.READY
      querySubscriptionProducts()
      queryExistingPurchases(isExplicitRestore = false)
      return
    }
    _purchaseStatus.value = BillingPurchaseStatus.CONNECTING
    _billingAvailability.value = BillingAvailability.CONNECTING
    try {
      billingClient.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
          if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isBillingReady.value = true
            _billingAvailability.value = BillingAvailability.READY
            _billingError.value = null
            if (_purchaseStatus.value == BillingPurchaseStatus.CONNECTING) {
              _purchaseStatus.value = BillingPurchaseStatus.IDLE
            }
            querySubscriptionProducts()
            queryExistingPurchases(isExplicitRestore = false)
          } else {
            _isBillingReady.value = false
            _billingAvailability.value = BillingAvailability.UNAVAILABLE_PREVIEW
            if (_purchaseStatus.value == BillingPurchaseStatus.CONNECTING) {
              _purchaseStatus.value = BillingPurchaseStatus.IDLE
            }
            Log.w(
              "BillingRepository",
              "Google Play Billing setup returned ${billingResult.responseCode}: ${billingResult.debugMessage}"
            )
          }
        }

        override fun onBillingServiceDisconnected() {
          _isBillingReady.value = false
          _billingAvailability.value = BillingAvailability.UNAVAILABLE_PREVIEW
        }
      })
    } catch (e: Exception) {
      _isBillingReady.value = false
      _billingAvailability.value = BillingAvailability.UNAVAILABLE_PREVIEW
      _purchaseStatus.value = BillingPurchaseStatus.IDLE
      Log.w("BillingRepository", "Billing connection exception: ${e.message}")
    }
  }

  fun querySubscriptionProducts() {
    if (!billingClient.isReady) return
    val productList = listOf(
      SubscriptionPlan.PRODUCT_ID_PRO,
      SubscriptionPlan.PRODUCT_ID_MONTHLY,
      SubscriptionPlan.PRODUCT_ID_YEARLY
    ).distinct().map { productId ->
      QueryProductDetailsParams.Product.newBuilder()
        .setProductId(productId)
        .setProductType(BillingClient.ProductType.SUBS)
        .build()
    }

    val params = QueryProductDetailsParams.newBuilder()
      .setProductList(productList)
      .build()

    billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
      if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        val detailsList = queryProductDetailsResult.productDetailsList
        _productDetailsList.value = detailsList
        rebuildPricingMapFromLoadedProducts()
      }
    }
  }

  private fun selectOfferForProduct(
    details: ProductDetails,
    preferTrialOffer: Boolean,
    preferredBasePlanId: String? = null
  ): Triple<ProductDetails.SubscriptionOfferDetails?, ProductDetails.SubscriptionOfferDetails?, ProductDetails.SubscriptionOfferDetails?> {
    val allOffers = details.subscriptionOfferDetails.orEmpty()
    val matchingBaseOffers = when {
      !preferredBasePlanId.isNullOrBlank() -> {
        allOffers.filter { it.basePlanId.equals(preferredBasePlanId, ignoreCase = true) }
          .ifEmpty { allOffers }
      }
      details.productId == SubscriptionPlan.PRODUCT_ID_MONTHLY -> {
        allOffers.filter { it.basePlanId.equals(SubscriptionPlan.BASE_PLAN_MONTHLY, ignoreCase = true) }
          .ifEmpty { allOffers }
      }
      else -> {
        allOffers.filter { offer ->
          offer.basePlanId.equals(SubscriptionPlan.BASE_PLAN_YEARLY, ignoreCase = true) ||
            offer.pricingPhases.pricingPhaseList.any {
              it.billingPeriod.contains("Y", ignoreCase = true)
            }
        }.ifEmpty { allOffers }
      }
    }

    val trialOffer = matchingBaseOffers.firstOrNull { offer ->
      offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
    }
    val baseOffer = matchingBaseOffers.firstOrNull { offer ->
      offer.pricingPhases.pricingPhaseList.none { it.priceAmountMicros == 0L }
    } ?: matchingBaseOffers.firstOrNull()

    val chosen = if (preferTrialOffer && trialOffer != null) trialOffer else (baseOffer ?: trialOffer)
    return Triple(chosen, baseOffer, trialOffer)
  }

  private fun rebuildPricingMapFromLoadedProducts() {
    val detailsList = _productDetailsList.value
    val updatedMap = defaultCatalogPricing(hasUsedFreeTrial = userHasUsedFreeTrial).toMutableMap()
    val isEligibleAccount = !userHasUsedFreeTrial

    var liveMonthlyMicros = 0L
    var liveMonthlyCurrency = "USD"
    var liveYearlyMicros = 0L
    var liveYearlyCurrency = "USD"

    for (details in detailsList) {
      val (selectedOffer, baseOffer, trialOffer) = selectOfferForProduct(details, preferTrialOffer = isEligibleAccount)
      val paidPhase = selectedOffer?.pricingPhases?.pricingPhaseList?.firstOrNull {
        it.priceAmountMicros > 0L
      } ?: selectedOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()

      val trialPhase = if (isEligibleAccount) {
        trialOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros == 0L }
      } else {
        null
      }

      if (paidPhase != null) {
        val isYearly = details.productId == SubscriptionPlan.PRODUCT_ID_YEARLY ||
          paidPhase.billingPeriod.contains("Y", ignoreCase = true)
        val suffix = if (isYearly) "/year" else "/month"
        val cycleSuffix = if (isYearly) "/year" else "/month"
        val freqLabel = if (isYearly) "Yearly" else "Monthly"
        val trialDays = if (trialPhase != null) parseIsoPeriodToDays(trialPhase.billingPeriod) else 7
        val hasTrialOffer = isEligibleAccount && trialPhase != null
        val firstBillStr = computeFirstBillingDateString(trialDays)
        val actualBasePlanId = selectedOffer?.basePlanId ?: baseOffer?.basePlanId

        if (isYearly) {
          liveYearlyMicros = paidPhase.priceAmountMicros
          liveYearlyCurrency = paidPhase.priceCurrencyCode
        } else {
          liveMonthlyMicros = paidPhase.priceAmountMicros
          liveMonthlyCurrency = paidPhase.priceCurrencyCode
        }

        val eqMonthly = if (isYearly) {
          formatMonthlyEquivalentFromMicros(paidPhase.priceAmountMicros, paidPhase.priceCurrencyCode)
        } else {
          "${paidPhase.formattedPrice}/month"
        }

        val info = PlanPricingInfo(
          productId = details.productId,
          basePlanId = actualBasePlanId,
          formattedPrice = "${paidPhase.formattedPrice}$suffix",
          recurringPriceRaw = paidPhase.formattedPrice,
          priceAmountMicros = paidPhase.priceAmountMicros,
          priceCurrencyCode = paidPhase.priceCurrencyCode,
          equivalentMonthlyFormatted = eqMonthly,
          savingsPercent = if (isYearly) 33 else 0,
          billingPeriodIso = paidPhase.billingPeriod,
          billingFrequencyLabel = freqLabel,
          billingCycleSuffix = cycleSuffix,
          hasFreeTrial = false,
          trialDurationDays = 0,
          trialPeriodLabel = null,
          firstBillingDateFormatted = "",
          billingPeriodText = "Billed ${freqLabel.lowercase(Locale.US)} at ${paidPhase.formattedPrice}$cycleSuffix • Cancel anytime",
          offerToken = selectedOffer?.offerToken,
          baseOfferToken = baseOffer?.offerToken ?: selectedOffer?.offerToken,
          isLiveFromPlayStore = true
        )
        updatedMap[details.productId] = info
        if (details.productId == SubscriptionPlan.PRODUCT_ID_PRO) {
          val aliasKey = if (isYearly) SubscriptionPlan.PRODUCT_ID_YEARLY else SubscriptionPlan.PRODUCT_ID_MONTHLY
          updatedMap[aliasKey] = info.copy(productId = aliasKey)
        }
      }
    }

    // Recompute exact percentage savings dynamically when Play provides live prices
    if (liveMonthlyMicros > 0L && liveYearlyMicros > 0L) {
      val computedSavings = calculateYearlySavingsPercent(
        monthlyMicros = liveMonthlyMicros,
        yearlyMicros = liveYearlyMicros,
        monthlyCurrency = liveMonthlyCurrency,
        yearlyCurrency = liveYearlyCurrency
      )
      updatedMap[SubscriptionPlan.PRODUCT_ID_YEARLY]?.let { yearlyInfo ->
        updatedMap[SubscriptionPlan.PRODUCT_ID_YEARLY] = yearlyInfo.copy(savingsPercent = computedSavings)
      }
    }

    _planPricingMap.value = updatedMap
  }

  fun queryExistingPurchases(
    isExplicitRestore: Boolean = false,
    onResult: ((Boolean, String) -> Unit)? = null
  ) {
    if (!billingClient.isReady) {
      _billingAvailability.value = BillingAvailability.UNAVAILABLE_PREVIEW
      if (isExplicitRestore) {
        startBillingConnection()
        val msg = "Google Play Billing is currently unavailable in this preview. Install from Google Play Console internal testing on a Play-certified device to restore purchases."
        _statusMessage.value = msg
        onResult?.invoke(false, msg)
      }
      return
    }
    val params = QueryPurchasesParams.newBuilder()
      .setProductType(BillingClient.ProductType.SUBS)
      .build()

    billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
      if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        val activePurchases = purchasesList.filter {
          it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        val pendingPurchases = purchasesList.filter {
          it.purchaseState == Purchase.PurchaseState.PENDING
        }
        if (activePurchases.isNotEmpty()) {
          for (purchase in activePurchases) {
            handlePurchase(purchase, isRestore = isExplicitRestore, onRestoreCallback = onResult)
          }
        } else if (pendingPurchases.isNotEmpty()) {
          handlePurchase(pendingPurchases.first(), isRestore = isExplicitRestore, onRestoreCallback = onResult)
        } else {
          activePurchaseToken = null
          activeProductId = null
          onEntitlementRevoked?.invoke()
          if (isExplicitRestore) {
            _purchaseStatus.value = BillingPurchaseStatus.NO_ACTIVE_PURCHASES_FOUND
            val msg = "No active Google Play subscriptions found for this Google account."
            _statusMessage.value = msg
            onResult?.invoke(false, msg)
          }
        }
      } else if (isExplicitRestore) {
        _purchaseStatus.value = BillingPurchaseStatus.FAILED
        val msg = "Could not query Google Play subscriptions: ${billingResult.debugMessage}"
        _billingError.value = msg
        _statusMessage.value = msg
        onResult?.invoke(false, msg)
      }
    }
  }

  fun restorePurchases(onResult: ((Boolean, String) -> Unit)? = null) {
    _statusMessage.value = "Checking Google Play for active subscriptions..."
    queryExistingPurchases(isExplicitRestore = true, onResult = onResult)
  }

  /**
   * Launches the official Google Play subscription purchase sheet (supports Monthly <-> Yearly replacement).
   * Requires a signed-in Firebase UID (`obfuscatedExternalAccountId = firebaseUid`) and never simulates or fakes a purchase.
   */
  fun launchBillingFlow(
    activity: Activity?,
    productId: String = SubscriptionPlan.PRODUCT_ID_MONTHLY,
    isFreeTrial: Boolean = false
  ) {
    val currentFirebaseUid = runCatching {
      com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }.getOrNull()?.trim().orEmpty()

    if (!billingClient.isReady) {
      startBillingConnection()
      _billingAvailability.value = BillingAvailability.UNAVAILABLE_PREVIEW
      _purchaseStatus.value = BillingPurchaseStatus.IDLE
      _statusMessage.value =
        "Google Play Billing is not available in this preview environment. Test real subscriptions via a Play Console Internal Testing track on a Play-certified Android device."
      return
    }

    val requestedYearly = productId == SubscriptionPlan.PRODUCT_ID_YEARLY
    val desiredBasePlanId = if (requestedYearly) SubscriptionPlan.BASE_PLAN_YEARLY else SubscriptionPlan.BASE_PLAN_MONTHLY

    val details = _productDetailsList.value.find { it.productId == productId }
      ?: _productDetailsList.value.find { it.productId == SubscriptionPlan.PRODUCT_ID_PRO }
    if (activity == null || details == null) {
      querySubscriptionProducts()
      _purchaseStatus.value = BillingPurchaseStatus.FAILED
      val msg = "Subscription product '$productId' could not be loaded from Google Play yet. Please try again in a moment."
      _billingError.value = msg
      _statusMessage.value = msg
      return
    }

    val allowTrialForAccount = isFreeTrial && !userHasUsedFreeTrial
    val (selectedOffer, _, _) = selectOfferForProduct(
      details = details,
      preferTrialOffer = allowTrialForAccount,
      preferredBasePlanId = desiredBasePlanId
    )
    val hasZeroPricePhase = selectedOffer?.pricingPhases?.pricingPhaseList?.any {
      it.priceAmountMicros == 0L
    } == true
    pendingIsFreeTrialFlow = allowTrialForAccount && hasZeroPricePhase
    pendingRequestedBasePlanId = selectedOffer?.basePlanId ?: desiredBasePlanId

    val offerToken = selectedOffer?.offerToken ?: ""
    val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
      .setProductDetails(details)
      .apply {
        if (offerToken.isNotEmpty()) {
          setOfferToken(offerToken)
        }
      }
      .build()

    val flowParamsBuilder = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(listOf(productDetailsParams))

    // Bind Google Play purchase token to Firebase Auth UID (`obfuscatedExternalAccountId`)
    if (currentFirebaseUid.isNotEmpty()) {
      flowParamsBuilder.setObfuscatedAccountId(currentFirebaseUid)
    }

    val existingToken = activePurchaseToken
    val existingProduct = activeProductId
    if (!existingToken.isNullOrBlank() && !existingProduct.isNullOrBlank() && existingProduct != productId) {
      // Monthly -> Yearly upgrade uses WITH_TIME_PRORATION; Yearly -> Monthly downgrade uses DEFERRED
      val replacementMode = if (requestedYearly) {
        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
      } else {
        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
      }
      flowParamsBuilder.setSubscriptionUpdateParams(
        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
          .setOldPurchaseToken(existingToken)
          .setSubscriptionReplacementMode(replacementMode)
          .build()
      )
    }

    _purchaseStatus.value = BillingPurchaseStatus.LAUNCHING_FLOW
    val result = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
      _purchaseStatus.value = BillingPurchaseStatus.FAILED
      val errText = result.debugMessage.ifBlank {
        "Google Play Billing could not open the subscription screen (code ${result.responseCode})."
      }
      _billingError.value = errText
      _statusMessage.value = errText
    }
  }

  fun openGooglePlaySubscriptionManagement(
    contextOrActivity: Context? = context,
    productId: String? = null
  ) {
    val targetCtx = contextOrActivity ?: context
    val pkg = targetCtx.packageName
    val uri = if (!productId.isNullOrBlank()) {
      Uri.parse("https://play.google.com/store/account/subscriptions?sku=$productId&package=$pkg")
    } else {
      Uri.parse("https://play.google.com/store/account/subscriptions?package=$pkg")
    }
    try {
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      targetCtx.startActivity(intent)
    } catch (e: Exception) {
      Log.w("BillingRepository", "Could not open Google Play subscriptions page: ${e.message}")
    }
  }

  private fun mapProductToTier(productId: String?): String {
    return when {
      productId == SubscriptionPlan.PRODUCT_ID_YEARLY ||
        pendingRequestedBasePlanId.equals(SubscriptionPlan.BASE_PLAN_YEARLY, ignoreCase = true) -> "premium_yearly"
      productId == SubscriptionPlan.PRODUCT_ID_MONTHLY ||
        productId == SubscriptionPlan.PRODUCT_ID_PRO -> "premium_pro"
      else -> "premium_pro"
    }
  }

  private fun handlePurchase(
    purchase: Purchase,
    isRestore: Boolean,
    onRestoreCallback: ((Boolean, String) -> Unit)?
  ) {
    when (purchase.purchaseState) {
      Purchase.PurchaseState.PURCHASED -> {
        val productId = purchase.products.firstOrNull() ?: SubscriptionPlan.PRODUCT_ID_MONTHLY
        val tier = mapProductToTier(productId)
        val isTrialPurchase = pendingIsFreeTrialFlow && !userHasUsedFreeTrial && !isRestore
        if (isTrialPurchase) {
          userHasUsedFreeTrial = true
        }
        val purchaseTimeMs = purchase.purchaseTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        val periodDays = when {
          isTrialPurchase -> 7L
          tier == "premium_yearly" -> 365L
          else -> 30L
        }
        val computedExpiryMs = purchaseTimeMs + periodDays * 24L * 60L * 60L * 1000L
        activePurchaseToken = purchase.purchaseToken
        activeProductId = productId
        _isAutoRenewing.value = purchase.isAutoRenewing
        _verifiedPurchaseTimeMs.value = purchaseTimeMs

        val resolvedBasePlanId = getPlanPricingInfo(productId)?.basePlanId
          ?: pendingRequestedBasePlanId.ifBlank {
            if (productId == SubscriptionPlan.PRODUCT_ID_MONTHLY) SubscriptionPlan.BASE_PLAN_MONTHLY else ""
          }

        val verifiedDetails = VerifiedSubscriptionDetails(
          tier = tier,
          purchaseToken = purchase.purchaseToken,
          productId = productId,
          basePlanId = resolvedBasePlanId,
          isFreeTrial = isTrialPurchase,
          isAutoRenewing = purchase.isAutoRenewing,
          purchaseTimeEpochMs = purchaseTimeMs,
          expiryEpochMs = computedExpiryMs
        )

        if (!purchase.isAcknowledged) {
          val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
          billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
              dispatchVerifiedPurchase(verifiedDetails, isRestore, onRestoreCallback)
            } else {
              _purchaseStatus.value = BillingPurchaseStatus.FAILED
              val err = "Google Play purchase acknowledgment failed: ${billingResult.debugMessage}"
              _billingError.value = err
              _statusMessage.value = err
              onRestoreCallback?.invoke(false, err)
            }
          }
        } else {
          dispatchVerifiedPurchase(verifiedDetails, isRestore, onRestoreCallback)
        }
      }
      Purchase.PurchaseState.PENDING -> {
        _purchaseStatus.value = BillingPurchaseStatus.PENDING
        val msg = "Your Google Play payment is pending confirmation. Premium access will activate automatically once completed."
        _statusMessage.value = msg
        onRestoreCallback?.invoke(false, msg)
      }
      else -> {
        _purchaseStatus.value = BillingPurchaseStatus.IDLE
      }
    }
  }

  private fun dispatchVerifiedPurchase(
    details: VerifiedSubscriptionDetails,
    isRestore: Boolean,
    onRestoreCallback: ((Boolean, String) -> Unit)?
  ) {
    externalScope.launch(Dispatchers.Main) {
      _purchaseStatus.value = BillingPurchaseStatus.VERIFIED_ACTIVE
      _billingError.value = null
      val expiryText = formatEpochDateReadable(details.expiryEpochMs)
      val retentionNote = when {
        !details.isAutoRenewing && expiryText.isNotBlank() ->
          " (Canceled — access retained until $expiryText)"
        details.isFreeTrial && expiryText.isNotBlank() ->
          " (Free trial active — first charge on $expiryText)"
        else -> ""
      }
      val msg = if (isRestore) {
        "Active Google Play subscription restored$retentionNote."
      } else {
        "Google Play subscription confirmed$retentionNote!"
      }
      _statusMessage.value = msg
      rebuildPricingMapFromLoadedProducts()
      if (onSubscriptionDetailsVerified != null) {
        onSubscriptionDetailsVerified.invoke(details)
      } else {
        onEntitlementVerified(details.tier, details.purchaseToken)
      }
      onRestoreCallback?.invoke(true, msg)
    }
  }

  companion object {
    fun defaultCatalogPricing(hasUsedFreeTrial: Boolean = false): Map<String, PlanPricingInfo> {
      return mapOf(
        SubscriptionPlan.PRODUCT_ID_MONTHLY to PlanPricingInfo(
          productId = SubscriptionPlan.PRODUCT_ID_MONTHLY,
          basePlanId = SubscriptionPlan.BASE_PLAN_MONTHLY,
          formattedPrice = "$9.99 / month",
          recurringPriceRaw = "$9.99",
          priceAmountMicros = 9_990_000L,
          priceCurrencyCode = "USD",
          equivalentMonthlyFormatted = "$9.99/month",
          savingsPercent = 0,
          billingPeriodIso = "P1M",
          billingFrequencyLabel = "Monthly",
          billingCycleSuffix = "/month",
          hasFreeTrial = false,
          trialDurationDays = 7,
          trialPeriodLabel = null,
          billingPeriodText = "Billed monthly • Full Premium access • Cancel anytime",
          isLiveFromPlayStore = false
        ),
        SubscriptionPlan.PRODUCT_ID_YEARLY to PlanPricingInfo(
          productId = SubscriptionPlan.PRODUCT_ID_YEARLY,
          basePlanId = null,
          formattedPrice = "$79.99 / year",
          recurringPriceRaw = "$79.99",
          priceAmountMicros = 79_990_000L,
          priceCurrencyCode = "USD",
          equivalentMonthlyFormatted = "$6.67/month",
          savingsPercent = 33,
          billingPeriodIso = "P1Y",
          billingFrequencyLabel = "Yearly",
          billingCycleSuffix = "/year",
          hasFreeTrial = false,
          trialDurationDays = 7,
          trialPeriodLabel = null,
          billingPeriodText = "Equivalent to $6.67/month • Save 33% compared with 12 monthly payments",
          isLiveFromPlayStore = false
        )
      )
    }
  }
}
