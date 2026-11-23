package com.example.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiErrorHandler
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyManager
import com.example.ui.components.LingoCleanCard
import kotlinx.coroutines.delay

/**
 * Compact AdMob Banner pinned above the bottom navigation bar on allowed screens
 * (Learn/Library, Progress/Profile, and Listening Hub Home) for Free users only,
 * with 8.dp spacing above the bottom navigation bar so nothing is cut off or overlapped.
 *
 * Calls `AdService.shouldShowAds(userProfile)` first — Pro users NEVER see this banner
 * and no ad request is made for them. If the ad fails to load, collapses completely (0 height).
 */
@Composable
fun LingoBannerAdBar(
    userProfile: UserProfile?,
    placementName: String,
    onUpgradeToProClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adState by AdService.state.collectAsState()
    val isOnline by AiErrorHandler.isOnline.collectAsState()
    val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
    val entitlementState by AccessPolicyManager.entitlementState.collectAsState()

    val canShow = remember(
        userProfile,
        adState,
        isOnline,
        isEntitlementLoading,
        entitlementState
    ) {
        AdService.shouldShowAds(userProfile) && adState.bannerLoaded
    }
    if (!canShow) {
        return
    }

    val bannerUnitId = AdConfig.bannerAdUnitId
    val GlassSurface = Color(0xFF0E1526)
    val GlassBorder = Color(0x1AFFFFFF)
    val SecondaryText = Color(0xFFA8B3CF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("admob_banner_$placementName"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x266C63FF))
                    .border(1.dp, Color(0x556C63FF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("admob_banner_badge_$placementName")
            ) {
                Text(
                    text = "Ad",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (AdConfig.USE_TEST_ADS) "LingoSphere Pro · Zero Ads & Unlimited AI" else "Upgrade to LingoSphere Pro",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (AdConfig.USE_TEST_ADS) "Test Banner (${bannerUnitId.takeLast(10)})" else "Sponsored · Remove ads anytime",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        TextButton(
            onClick = onUpgradeToProClick,
            modifier = Modifier.testTag("admob_banner_remove_ads_$placementName")
        ) {
            Text(
                text = "Remove Ads",
                color = Color(0xFF22D3EE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Native Advanced Ad Card styled to match LingoSphere's `#070B14` dark glass UI,
 * placed between sections on Home, Listening Hub, and Podcasts, clearly labeled "Ad".
 *
 * Shown ONLY to Free users when `AdService.shouldShowAds(userProfile)` is true;
 * completely omitted (0px height, no empty gap) for Pro users or if the ad fails to load.
 */
@Composable
fun LingoNativeAdCard(
    userProfile: UserProfile?,
    placementKey: String,
    headline: String = "Master Fluency 3x Faster with Unlimited AI Speaking",
    bodyText: String = "Practice real-world conversations with Emma, unlock all audiobooks, and enjoy a 100% ad-free experience.",
    ctaText: String = "Go Pro · No Ads",
    onCtaClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adState by AdService.state.collectAsState()
    val isOnline by AiErrorHandler.isOnline.collectAsState()
    val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
    val entitlementState by AccessPolicyManager.entitlementState.collectAsState()

    val canShow = remember(
        userProfile,
        adState,
        isOnline,
        isEntitlementLoading,
        entitlementState
    ) {
        AdService.shouldShowAds(userProfile) && adState.nativeLoaded
    }
    if (!canShow) {
        return
    }

    val nativeUnitId = AdConfig.nativeAdvancedAdUnitId
    val SecondaryText = Color(0xFFA8B3CF)

    LingoCleanCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_native_card_$placementKey"),
        shape = RoundedCornerShape(24.dp),
        contentPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x26F5C451))
                            .border(1.dp, Color(0x66F5C451), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("admob_native_ad_label_$placementKey")
                    ) {
                        Text(
                            text = "Ad",
                            color = Color(0xFFF5C451),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (AdConfig.USE_TEST_ADS) {
                            "Google Native Advanced (${nativeUnitId.takeLast(10)})"
                        } else {
                            "Sponsored Recommendation"
                        },
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Sponsored",
                    tint = Color(0xFFF5C451),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = headline,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = bodyText,
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCtaClick,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF22D3EE)))
                    ),
                    modifier = Modifier.testTag("admob_native_cta_$placementKey")
                ) {
                    Text(
                        text = ctaText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Native Advanced Ad Card (`ca-app-pub-8471293855339658/2376700449`, test `ca-app-pub-3940256099942544/2247696110`)
 * styled specifically like a Book Card (`AudiobookCompactListItem`) for the Book & Audio Library list.
 *
 * - Inserted after every 8 books in the infinite scroll list without altering pagination or the book count.
 * - Clearly labeled `"Ad"` with a small `"Go Pro - No Ads"` button.
 * - Calls `AdService.shouldShowAds(userProfile)` first; if Pro or if the ad fails to load, collapses completely
 *   with zero empty gap.
 */
@Composable
fun LingoBookListNativeAdCard(
    userProfile: UserProfile?,
    slotIndex: Int,
    onGoProClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adState by AdService.state.collectAsState()
    val isOnline by AiErrorHandler.isOnline.collectAsState()
    val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
    val entitlementState by AccessPolicyManager.entitlementState.collectAsState()

    val canShow = remember(
        userProfile,
        adState,
        isOnline,
        isEntitlementLoading,
        entitlementState
    ) {
        AdService.shouldShowAds(userProfile) && adState.nativeLoaded
    }
    if (!canShow) {
        return
    }

    val nativeUnitId = AdConfig.nativeAdvancedAdUnitId
    val DeepNavyCard = Color(0xFF0F1B38)
    val DeepNavyBorder = Color(0xFF1E305C)
    val CyanAccent = Color(0xFF06B6D4)
    val AmberAccent = Color(0xFFF59E0B)
    val TextWhite = Color(0xFFFFFFFF)
    val TextLight = Color(0xFFCBD5E1)

    Card(
        onClick = onGoProClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_book_native_card_$slotIndex")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover-styled artwork tile (56x76dp matching AudiobookCompactListItem)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A254B),
                modifier = Modifier.size(width = 56.dp, height = 76.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6C63FF), Color(0xFF06B6D4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberAccent.copy(alpha = 0.22f),
                        border = BorderStroke(0.8.dp, AmberAccent.copy(alpha = 0.65f)),
                        modifier = Modifier.testTag("admob_book_native_badge_$slotIndex")
                    ) {
                        Text(
                            text = "Ad",
                            color = AmberAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = if (AdConfig.USE_TEST_ADS) {
                            "Native Advanced (${nativeUnitId.takeLast(10)})"
                        } else {
                            "LingoSphere Pro Library"
                        },
                        color = CyanAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Unlock Unlimited Audiobooks & Full Stories",
                    color = TextWhite,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Read & listen to every book with 100% zero ads",
                    color = TextLight,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onGoProClick,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF6C63FF), CyanAccent))
                ),
                modifier = Modifier.testTag("admob_book_native_cta_$slotIndex")
            ) {
                Text(
                    text = "Go Pro - No Ads",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Native Advanced Ad Card (`ca-app-pub-8471293855339658/2376700449`, test `ca-app-pub-3940256099942544/2247696110`)
 * for the Play & Learn (Games) Explore Games grid.
 *
 * - Inserted after every 4 game cards, spanning the full width.
 * - Styled like a Play & Learn game card (`Color(0xFF0D162B)`, `RoundedCornerShape(16.dp)`, `Color(0xFF1D2E52)` border).
 * - Clearly labeled `"Ad"` with a small `"Go Pro - No Ads"` button.
 * - Calls `AdService.shouldShowAds(userProfile)` first; if Pro or if the ad fails to load, collapses completely
 *   with zero empty gap.
 */
@Composable
fun LingoGameGridNativeAdCard(
    userProfile: UserProfile?,
    slotIndex: Int,
    onGoProClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adState by AdService.state.collectAsState()
    val isOnline by AiErrorHandler.isOnline.collectAsState()
    val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
    val entitlementState by AccessPolicyManager.entitlementState.collectAsState()

    val canShow = remember(
        userProfile,
        adState,
        isOnline,
        isEntitlementLoading,
        entitlementState
    ) {
        AdService.shouldShowAds(userProfile) && adState.nativeLoaded && !AdService.simulateAdLoadFailure
    }
    if (!canShow) {
        return
    }

    val nativeUnitId = AdConfig.nativeAdvancedAdUnitId
    val GamesCardNavy = Color(0xFF0D162B)
    val GamesBorder = Color(0xFF1D2E52)
    val GamesCyan = Color(0xFF38BDF8)
    val GamesAmber = Color(0xFFF59E0B)

    Card(
        onClick = onGoProClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
        border = BorderStroke(1.dp, GamesBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_game_native_card_$slotIndex")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GamesCyan.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = GamesCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GamesAmber.copy(alpha = 0.22f),
                        border = BorderStroke(0.8.dp, GamesAmber.copy(alpha = 0.65f)),
                        modifier = Modifier.testTag("admob_game_native_badge_$slotIndex")
                    ) {
                        Text(
                            text = "Ad",
                            color = GamesAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = if (AdConfig.USE_TEST_ADS) {
                            "Native Advanced (${nativeUnitId.takeLast(10)})"
                        } else {
                            "LingoSphere Pro Arcade"
                        },
                        color = GamesCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Unlock All 500 Game Levels & Zero Ads",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Unlimited hints, brain games & speaking quests",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onGoProClick,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF6C63FF), GamesCyan))
                ),
                modifier = Modifier.testTag("admob_game_native_cta_$slotIndex")
            ) {
                Text(
                    text = "Go Pro - No Ads",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Small floating/header countdown chip ("Free trial 04:32") displayed during an active
 * 5-minute rewarded ad session (AI Tutor Emma, Speak, Roleplay, Camera Lens, AI Labs).
 */
@Composable
fun LingoRewardedTrialCountdownChip(
    modifier: Modifier = Modifier
) {
    val adState by AdService.state.collectAsState()
    AnimatedVisibility(
        visible = adState.isTimedSessionActive && AdService.shouldShowAds(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xE611192E))
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))),
                    RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("rewarded_trial_countdown_chip"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Free trial timer",
                tint = Color(0xFF22D3EE),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = adState.formattedTimedSessionCountdown,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Root host overlay that renders:
 * 1. Google UMP (GDPR / Privacy) Consent modal before loading ads
 * 2. Rewarded Ad player (with watch countdown + early-close warning that denies reward + reward callback button)
 * 3. Interstitial Ad modal at natural breaks
 * 4. Rewarded Interstitial opt-in prompt after lesson completion
 * 5. App Open Ad on foreground resume
 */
@Composable
fun LingoFullScreenAdOverlayHost(
    userProfile: UserProfile?
) {
    val adState by AdService.state.collectAsState()
    val isOnline by AiErrorHandler.isOnline.collectAsState()
    val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
    val entitlementState by AccessPolicyManager.entitlementState.collectAsState()

    val isProOrBlocked = remember(
        userProfile,
        adState,
        isOnline,
        isEntitlementLoading,
        entitlementState
    ) {
        AccessPolicyManager.isUserVerifiedPro(userProfile) ||
            adState.isUserVerifiedPro ||
            (!adState.showUmpConsentModal && !AdService.shouldShowAds(userProfile))
    }
    if (isProOrBlocked) return

    // 1. GDPR / UMP Privacy Consent Modal
    if (adState.showUmpConsentModal) {
        Dialog(
            onDismissRequest = { AdService.submitUmpConsent(consentGranted = true) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC070B14))
                    .padding(24.dp)
                    .testTag("ump_consent_dialog"),
                contentAlignment = Alignment.Center
            ) {
                LingoCleanCard(
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy & Ad Consent",
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Privacy & Ad Preferences",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LingoSphere Free uses Google AdMob to keep language lessons and books free. You can choose personalized ads or non-personalized ads anytime, or upgrade to LingoSphere Pro for a 100% ad-free experience.",
                            color = Color(0xFFA8B3CF),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = { AdService.submitUmpConsent(consentGranted = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ump_consent_accept_btn")
                        ) {
                            Text("Accept & Continue", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { AdService.submitUmpConsent(consentGranted = false) },
                            modifier = Modifier.testTag("ump_consent_manage_btn")
                        ) {
                            Text(
                                "Use Non-Personalized Ads Only",
                                color = Color(0xFFA8B3CF),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Full-Screen Ad Presentation (Rewarded, Interstitial, Rewarded Interstitial, App Open)
    if (!AdService.shouldShowAds(userProfile)) return
    val presentation = adState.activeFullScreenAd ?: return

    Dialog(
        onDismissRequest = {
            when (presentation) {
                is FullScreenAdPresentation.Rewarded -> AdService.dismissActiveRewardedAdEarly()
                is FullScreenAdPresentation.Interstitial -> presentation.onDismissed()
                is FullScreenAdPresentation.RewardedInterstitialPrompt -> presentation.onDecline()
                is FullScreenAdPresentation.AppOpen -> presentation.onDismissed()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        when (presentation) {
            is FullScreenAdPresentation.Rewarded -> {
                RewardedAdPlayerOverlay(presentation = presentation)
            }
            is FullScreenAdPresentation.Interstitial -> {
                InterstitialAdOverlay(presentation = presentation)
            }
            is FullScreenAdPresentation.RewardedInterstitialPrompt -> {
                RewardedInterstitialPromptOverlay(presentation = presentation)
            }
            is FullScreenAdPresentation.AppOpen -> {
                AppOpenAdOverlay(presentation = presentation)
            }
        }
    }
}

@Composable
private fun RewardedAdPlayerOverlay(
    presentation: FullScreenAdPresentation.Rewarded
) {
    var secondsRemaining by remember(presentation) {
        mutableIntStateOf(presentation.watchDurationSeconds.coerceAtLeast(1))
    }
    var rewardEarned by remember(presentation) { mutableStateOf(false) }

    LaunchedEffect(presentation) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        rewardEarned = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070B14))
            .padding(20.dp)
            .testTag("rewarded_ad_overlay"),
        contentAlignment = Alignment.Center
    ) {
        LingoCleanCard(
            shape = RoundedCornerShape(28.dp),
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33F5C451))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (AdConfig.USE_TEST_ADS) "Ad · Google Test Rewarded" else "Ad · Rewarded Video",
                                color = Color(0xFFF5C451),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (rewardEarned) {
                                AdService.completeActiveRewardedAdWithReward()
                            } else {
                                // Closing early grants NO reward
                                AdService.dismissActiveRewardedAdEarly()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rewarded_ad_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ad",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF22D3EE)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (rewardEarned) Icons.Default.LockOpen else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = if (rewardEarned) {
                        "Reward Unlocked: ${presentation.featureType.displayName}"
                    } else {
                        "Unlocking ${presentation.featureType.displayName}..."
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (rewardEarned) {
                        if (presentation.featureType.isTimedFiveMinSession) {
                            "Your 5-minute Premium session is ready!"
                        } else {
                            "Your single-session Premium access is ready!"
                        }
                    } else {
                        "Watch until the timer finishes to unlock (${secondsRemaining}s remaining). Closing early gives no reward."
                    },
                    color = Color(0xFFA8B3CF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Text(
                    text = "Ad Unit: ${presentation.adUnitId}",
                    color = Color(0xFF6E7B99),
                    fontSize = 11.sp
                )

                val progress = if (presentation.watchDurationSeconds <= 0) 1f else {
                    (presentation.watchDurationSeconds - secondsRemaining).toFloat() /
                        presentation.watchDurationSeconds.toFloat()
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = Color(0xFF22D3EE),
                    trackColor = Color(0x26FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                )

                Button(
                    onClick = { AdService.completeActiveRewardedAdWithReward() },
                    enabled = rewardEarned || AdConfig.USE_TEST_ADS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C63FF),
                        disabledContainerColor = Color(0x336C63FF)
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("rewarded_ad_claim_reward_btn")
                ) {
                    Text(
                        text = if (rewardEarned) "Continue to ${presentation.featureType.displayName}" else "Complete Ad & Unlock Now",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { AdService.dismissActiveRewardedAdEarly() },
                    modifier = Modifier.testTag("rewarded_ad_close_early_btn")
                ) {
                    Text(
                        text = "Close ad early (no reward)",
                        color = Color(0xFFA8B3CF),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InterstitialAdOverlay(
    presentation: FullScreenAdPresentation.Interstitial
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070B14))
            .padding(20.dp)
            .testTag("interstitial_ad_overlay"),
        contentAlignment = Alignment.Center
    ) {
        LingoCleanCard(
            shape = RoundedCornerShape(28.dp),
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ad · Interstitial (${presentation.adUnitId.takeLast(10)})",
                        color = Color(0xFFF5C451),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = presentation.onDismissed,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("interstitial_ad_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Ad", tint = Color.White)
                    }
                }
                Text(
                    text = "Great progress on your lesson!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upgrade to LingoSphere Pro for zero interruptions, unlimited AI conversations with Emma, and full offline books.",
                    color = Color(0xFFA8B3CF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Button(
                    onClick = presentation.onDismissed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("interstitial_ad_continue_btn")
                ) {
                    Text("Continue Learning", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RewardedInterstitialPromptOverlay(
    presentation: FullScreenAdPresentation.RewardedInterstitialPrompt
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6070B14))
            .padding(20.dp)
            .testTag("rewarded_interstitial_prompt_overlay"),
        contentAlignment = Alignment.Center
    ) {
        LingoCleanCard(
            shape = RoundedCornerShape(28.dp),
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Watch an ad to earn a bonus",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = presentation.bonusDescription,
                    color = Color(0xFFA8B3CF),
                    fontSize = 14.sp
                )
                Button(
                    onClick = presentation.onAcceptWatch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rewarded_interstitial_accept_btn")
                ) {
                    Text("Watch Ad & Earn Bonus", color = Color.White, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = presentation.onDecline,
                    modifier = Modifier.testTag("rewarded_interstitial_decline_btn")
                ) {
                    Text("No thanks, continue", color = Color(0xFFA8B3CF))
                }
            }
        }
    }
}

@Composable
private fun AppOpenAdOverlay(
    presentation: FullScreenAdPresentation.AppOpen
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070B14))
            .padding(20.dp)
            .testTag("app_open_ad_overlay"),
        contentAlignment = Alignment.Center
    ) {
        LingoCleanCard(
            shape = RoundedCornerShape(28.dp),
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ad · App Open (${presentation.adUnitId.takeLast(10)})",
                    color = Color(0xFFF5C451),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome back to LingoSphere",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = presentation.onDismissed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("app_open_ad_continue_btn")
                ) {
                    Text("Continue to App", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
