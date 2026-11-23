package com.example.data.model

data class SubscriptionPlan(
  val productId: String,
  val basePlanId: String? = null,
  val tierCode: String,
  val title: String,
  val priceDisplay: String,
  val billingCycle: String,
  val badge: String? = null,
  val description: String,
  val features: List<String>,
  val isFreePlan: Boolean = false
) {
  companion object {
    // Canonical single Play Console subscription product ID with two base plans ("monthly" P1M and "yearly" P1Y)
    const val PRODUCT_ID_PRO = "lingosphere_pro"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    // Legacy product ID constants preserved for seamless compatibility across BillingClient queries and tests
    const val PRODUCT_ID_MONTHLY = "lingosphere_premium"
    const val PRODUCT_ID_YEARLY = "lingosphere_yearly"
    const val PRODUCT_ID_PRO_MONTHLY = "lingosphere_premium"

    val FREE_PLAN = SubscriptionPlan(
      productId = "free_tier",
      basePlanId = null,
      tierCode = "free",
      title = "Free",
      priceDisplay = "$0",
      billingCycle = "forever",
      badge = "FREE PLAN",
      description = "5 books per month, saved flashcard review, basic listening, and Language Exchange.",
      isFreePlan = true,
      features = listOf(
        "Books & Audiobooks: 5 distinct books per month (LibriVox & Gutendex)",
        "Review saved flashcards offline & in SRS without AI",
        "Basic non-AI listening playback and starter language games",
        "Language Exchange: human partner browsing, text chat & calls",
        "Speaking Rooms: listen & join public community rooms"
      )
    )

    val PREMIUM_MONTHLY_PLAN = SubscriptionPlan(
      productId = PRODUCT_ID_MONTHLY,
      basePlanId = BASE_PLAN_MONTHLY,
      tierCode = "premium_pro",
      title = "LingoSphere Pro Monthly",
      priceDisplay = "$9.99",
      billingCycle = "per month",
      badge = "MONTHLY PRO",
      description = "Unlimited structured lessons, AI Tutor Emma, Speaking Labs, Roleplay, and unlimited books.",
      features = listOf(
        "All 500+ sequential CEFR A1–C2 structured lessons",
        "AI Tutor Emma: 15 min/day & 150 min/month voice/video + AI text chat",
        "AI Roleplay Studio, Exams, Interview Coach & Speaking Labs",
        "Pronunciation Coach, Writing & Grammar AI feedback, Camera Lens & Immersion",
        "Unlimited LibriVox audiobooks & Gutendex reading books + AI summaries",
        "All 15 AI tutor voices, smart matching & full fluency analytics"
      )
    )

    val PREMIUM_YEARLY_PLAN = SubscriptionPlan(
      productId = PRODUCT_ID_YEARLY,
      basePlanId = BASE_PLAN_YEARLY,
      tierCode = "premium_yearly",
      title = "LingoSphere Pro Yearly",
      priceDisplay = "$79.99",
      billingCycle = "per year",
      badge = "BEST VALUE · SAVE 33%",
      description = "Best value annual LingoSphere Pro membership (~$6.67/month, save 33% vs monthly).",
      features = listOf(
        "Everything in LingoSphere Pro Monthly",
        "Billed annually ($79.99/year — equivalent to $6.67/month, save 33%)",
        "All 500+ structured lessons & unlimited LibriVox/Gutendex books",
        "15 min/day (150 min/mo) AI Tutor Emma + all AI Speaking Labs",
        "Cancel anytime in Google Play Subscriptions"
      )
    )

    // Alias for legacy callers
    val PREMIUM_PRO_MONTHLY_PLAN = PREMIUM_MONTHLY_PLAN

    // Strictly two plans exist: Free and LingoSphere Pro (Monthly / Yearly billing options)
    val ALL_PLANS = listOf(
      FREE_PLAN,
      PREMIUM_MONTHLY_PLAN,
      PREMIUM_YEARLY_PLAN
    )

    val PAID_GOOGLE_PLAY_PLANS = listOf(
      PREMIUM_MONTHLY_PLAN,
      PREMIUM_YEARLY_PLAN
    )
  }
}
