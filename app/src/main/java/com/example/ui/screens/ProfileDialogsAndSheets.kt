package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountryInfo
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.repository.BillingPurchaseStatus
import com.example.data.repository.BillingRepository
import com.example.ui.components.LingoAvatar
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold

@Composable
internal fun EditLearnerProfileDialog(
  userId: String,
  currentAvatarUrl: String,
  initialName: String,
  initialCountry: String,
  initialFlag: String,
  initialBio: String,
  initialInterests: List<String>,
  isSaving: Boolean = false,
  onOpenPhotoPicker: () -> Unit,
  onRemovePhoto: () -> Unit,
  onDismiss: () -> Unit,
  onSave: (name: String, country: String, flag: String, bio: String, interests: List<String>) -> Unit
) {
  var name by remember(initialName) { mutableStateOf(initialName) }
  var selectedCountry by remember(initialCountry) { mutableStateOf(initialCountry) }
  var selectedFlag by remember(initialFlag) { mutableStateOf(initialFlag) }
  var bio by remember(initialBio) { mutableStateOf(initialBio) }
  var interests by remember(initialInterests) { mutableStateOf(initialInterests) }
  var nameError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0B1936),
    titleContentColor = Color.White,
    textContentColor = Color(0xFFE2E8F0),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
        }
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .testTag("edit_profile_dialog_content")
      ) {
        // Avatar + Photo Controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          LingoAvatar(
            name = name.ifBlank { "L" },
            userId = userId,
            size = 60.dp,
            customImageUrl = currentAvatarUrl,
            showCameraBadge = true,
            onClick = onOpenPhotoPicker
          )
          Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            OutlinedButton(
              onClick = onOpenPhotoPicker,
              border = BorderStroke(1.dp, LingoCyan),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_change_photo_button")
            ) {
              Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (currentAvatarUrl.isNotBlank()) "Replace Photo" else "Upload Photo",
                color = LingoCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
            if (currentAvatarUrl.isNotBlank()) {
              TextButton(
                onClick = onRemovePhoto,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("dialog_remove_photo_button")
              ) {
                Text(
                  text = "Remove Photo",
                  color = LingoCoral,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(40)
            nameError = null
          },
          label = { Text("Display Name") },
          singleLine = true,
          isError = nameError != null,
          supportingText = nameError?.let { err -> { Text(err, color = LingoCoral, fontSize = 11.sp) } },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = Color(0xFF1E3A6E),
            focusedLabelColor = LingoCyan,
            unfocusedLabelColor = Color(0xFF94A3B8)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_name_input")
        )

        Text("Country & Region", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF93C5FD))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
        ) {
          CountryInfo.COUNTRIES.forEach { c ->
            val sel = selectedCountry == c.name
            OutlinedButton(
              onClick = {
                selectedCountry = c.name
                selectedFlag = c.flagEmoji
              },
              border = BorderStroke(1.dp, if (sel) LingoCyan else Color(0xFF1E3A6E)),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (sel) LingoElectricBlue.copy(alpha = 0.3f) else Color(0xFF071226)
              ),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("country_option_${c.name.replace(" ", "_")}")
            ) {
              Text(
                text = "${c.flagEmoji} ${c.name}",
                fontSize = 11.5.sp,
                color = if (sel) Color.White else Color(0xFFCBD5E1),
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it.take(160) },
          label = { Text("Short Biography") },
          minLines = 2,
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = Color(0xFF1E3A6E),
            focusedLabelColor = LingoCyan,
            unfocusedLabelColor = Color(0xFF94A3B8)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_bio_input")
        )

        Text("Conversation Interests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF93C5FD))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
        ) {
          CountryInfo.INTEREST_TOPICS.forEach { topic ->
            val isSelected = interests.contains(topic)
            OutlinedButton(
              onClick = {
                interests = if (isSelected && interests.size > 1) {
                  interests - topic
                } else if (!isSelected && interests.size < 5) {
                  interests + topic
                } else {
                  interests
                }
              },
              border = BorderStroke(1.dp, if (isSelected) LingoCyan else Color(0xFF1E3A6E)),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) LingoCyan.copy(alpha = 0.18f) else Color(0xFF071226)
              ),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = topic,
                fontSize = 11.sp,
                color = if (isSelected) LingoCyan else Color(0xFFCBD5E1),
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val trimmedName = name.trim()
          if (trimmedName.isBlank()) {
            nameError = "Please enter your display name"
          } else {
            onSave(trimmedName, selectedCountry, selectedFlag, bio.trim(), interests)
          }
        },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.testTag("save_profile_changes_button")
      ) {
        if (isSaving) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text("Save Changes", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8), maxLines = 1, softWrap = false)
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionManagementBottomSheet(
  userProfile: UserProfile,
  billingRepository: BillingRepository?,
  onDismiss: () -> Unit,
  onOpenFullHub: () -> Unit,
  onPurchase: (productId: String, isTrial: Boolean) -> Unit,
  onRestorePurchases: () -> Unit,
  onCancelSubscriptionToFree: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val purchaseStatus = billingRepository?.purchaseStatus?.collectAsState()?.value ?: BillingPurchaseStatus.IDLE
  val statusMessage = billingRepository?.statusMessage?.collectAsState()?.value
  val billingError = billingRepository?.billingError?.collectAsState()?.value

  val yearlyPricing = remember(billingRepository) {
    billingRepository?.getPlanPricingInfo(SubscriptionPlan.PRODUCT_ID_YEARLY)
  }
  val proPricing = remember(billingRepository) {
    billingRepository?.getPlanPricingInfo(SubscriptionPlan.PRODUCT_ID_PRO_MONTHLY)
  }

  val isVerifiedPremium = userProfile.isEntitlementVerified
  val isYearlyActive = isVerifiedPremium && userProfile.subscriptionTier == "premium_yearly"
  val isProActive = isVerifiedPremium && userProfile.subscriptionTier != "premium_yearly"
  val isTrialEligible = false

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF08142C),
    contentColor = Color.White,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp)
        .padding(bottom = 28.dp)
        .testTag("subscription_bottom_sheet")
    ) {
      // Sheet Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isVerifiedPremium) LingoEmerald.copy(alpha = 0.2f) else LingoElectricBlue.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isVerifiedPremium) Icons.Default.Verified else Icons.Default.Star,
              contentDescription = null,
              tint = if (isVerifiedPremium) LingoEmerald else LingoCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (isVerifiedPremium) "Manage LingoSphere Subscription" else "LingoSphere Subscription Plans",
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (isVerifiedPremium) {
                "Verified Active Plan: ${userProfile.planDisplayName}"
              } else {
                "LingoSphere Pro ($9.99/mo)"
              },
              fontSize = 12.sp,
              color = Color(0xFF93C5FD)
            )
          }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
        }
      }

      // Purchase Status Banner (Pending, Canceled, Expired, Failed, Verified)
      if (!statusMessage.isNullOrBlank() || !billingError.isNullOrBlank()) {
        val bannerColor = when (purchaseStatus) {
          BillingPurchaseStatus.VERIFIED_ACTIVE -> LingoEmerald
          BillingPurchaseStatus.PENDING -> LingoAmber
          BillingPurchaseStatus.FAILED, BillingPurchaseStatus.EXPIRED -> LingoCoral
          else -> LingoCyan
        }
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.14f)),
          border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("billing_status_banner")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = bannerColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = statusMessage ?: billingError.orEmpty(),
              fontSize = 12.sp,
              color = Color.White,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 1. PRO Monthly Card ($9.99/mo — Best for AI Speaking)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isProActive) Color(0xFF1A253A) else Color(0xFF101F3F)
        ),
        border = BorderStroke(
          width = 1.5.dp,
          color = if (isProActive) LingoEmerald else LingoGold
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("plan_card_pro_monthly")
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "MONTHLY PREMIUM",
                  fontWeight = FontWeight.ExtraBold,
                  color = LingoCyan,
                  fontSize = 14.sp
                )
                if (isProActive) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(LingoEmerald)
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = "CURRENT PLAN",
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color(0xFF040B1A),
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
              Text(
                text = proPricing?.billingPeriodText
                  ?: "Billed monthly • Full Premium access • Cancel anytime",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = proPricing?.formattedPrice ?: "$9.99 / month",
              fontWeight = FontWeight.ExtraBold,
              color = LingoCyan,
              fontSize = 14.sp,
              maxLines = 1,
              softWrap = false
            )
          }

          if (isProActive) {
            OutlinedButton(
              onClick = {},
              enabled = false,
              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.6f)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Current Active Plan",
                color = LingoEmerald,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
              )
            }
          } else {
            Button(
              onClick = { onPurchase(SubscriptionPlan.PRODUCT_ID_PRO_MONTHLY, false) },
              colors = ButtonDefaults.buttonColors(containerColor = LingoGold, contentColor = Color(0xFF040B1A)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("select_pro_plan_button")
            ) {
              Text(
                text = "Choose Monthly (${proPricing?.formattedPrice ?: "$9.99 / month"})",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // 2. YEARLY PREMIUM Card (Best Value — Save 33%)
      val yearlySavingsPct = (yearlyPricing?.savingsPercent ?: 33).coerceAtLeast(0)
      val yearlySavingsLabel = if (yearlySavingsPct > 0) "SAVE $yearlySavingsPct%" else "BEST VALUE"
      val yearlyEqText = yearlyPricing?.equivalentMonthlyFormatted ?: "$6.67/month"
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isYearlyActive) Color(0xFF0E293C) else Color(0xFF0F224A)
        ),
        border = BorderStroke(
          width = 1.5.dp,
          color = if (isYearlyActive) LingoEmerald else LingoGold.copy(alpha = 0.7f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("plan_card_yearly")
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "YEARLY PREMIUM",
                  fontWeight = FontWeight.ExtraBold,
                  color = LingoGold,
                  fontSize = 14.sp
                )
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isYearlyActive) LingoEmerald else LingoGold)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (isYearlyActive) "CURRENT PLAN" else yearlySavingsLabel,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF040B1A),
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
              Text(
                text = yearlyPricing?.billingPeriodText
                  ?: "Equivalent to $yearlyEqText • Save $yearlySavingsPct% compared with 12 monthly payments",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = yearlyPricing?.formattedPrice ?: "$79.99 / year",
              fontWeight = FontWeight.ExtraBold,
              color = LingoGold,
              fontSize = 14.sp,
              maxLines = 1,
              softWrap = false
            )
          }

          if (isYearlyActive) {
            OutlinedButton(
              onClick = {},
              enabled = false,
              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.6f)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Current Active Plan",
                color = LingoEmerald,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
              )
            }
          } else {
            Button(
              onClick = { onPurchase(SubscriptionPlan.PRODUCT_ID_YEARLY, false) },
              colors = ButtonDefaults.buttonColors(containerColor = LingoGold, contentColor = Color(0xFF040B1A)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("select_yearly_plan_button")
            ) {
              Text(
                text = if (isProActive) {
                  "Switch to Yearly ($yearlySavingsLabel)"
                } else {
                  "Choose Yearly (${yearlyPricing?.formattedPrice ?: "$79.99 / year"})"
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // Renewal Terms & Billing Disclosure
      Text(
        text = "Renewal & Billing Terms: Subscriptions are billed through your Google Play account and automatically renew at the end of each monthly or annual billing period unless canceled at least 24 hours before the renewal date. You can manage or cancel your subscription anytime in Google Play Subscriptions.",
        fontSize = 11.sp,
        color = Color(0xFF94A3B8),
        lineHeight = 15.sp
      )

      // Restore Purchases & Manage in Google Play Row
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedButton(
          onClick = onRestorePurchases,
          border = BorderStroke(1.dp, Color(0xFF334155)),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("restore_purchases_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Restore Purchases",
            color = LingoCyan,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = {
            try {
              val playSubsUri = Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
              context.startActivity(Intent(Intent.ACTION_VIEW, playSubsUri))
            } catch (_: Exception) {}
          },
          border = BorderStroke(1.dp, Color(0xFF334155)),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("manage_google_play_sub_button")
        ) {
          Text(
            text = "Google Play Subs",
            color = Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (isVerifiedPremium) {
        TextButton(
          onClick = onCancelSubscriptionToFree,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cancel_local_entitlement_button")
        ) {
          Text(
            text = "End Active Plan & Return to Free Tier",
            color = LingoCoral,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}

@Composable
internal fun DeleteAccountConfirmationDialog(
  userEmail: String,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0B1936),
    titleContentColor = Color.White,
    textContentColor = Color(0xFFE2E8F0),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LingoCoral)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete Account & Data?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "This action permanently deletes your LingoSphere account${if (userEmail.isNotBlank()) " ($userEmail)" else ""} and removes all eligible data from our cloud backend and this device:",
          fontSize = 12.5.sp,
          color = Color.White,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "• User profile, biography, photos, and language settings\n" +
            "• Completed lessons, XP, streaks, and spaced-repetition flashcards\n" +
            "• Speaking session transcripts, pronunciation diagnostics & AI memory\n" +
            "• Community profile, friend connections, blocks, and device tokens\n" +
            "• Firebase Authentication login record",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1),
          lineHeight = 17.sp
        )
        HorizontalDivider(color = Color(0xFF1E3A6E))
        Row(verticalAlignment = Alignment.Top) {
          Icon(Icons.Default.Security, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Legal Retention Notice: In compliance with tax, financial audit, and fraud-prevention laws, anonymized Google Play billing transaction records are retained by payment processors for the statutory retention period (typically 5–7 years) and are unlinked from your personal profile. If you have an active subscription, please also cancel it in Google Play Subscriptions.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 15.sp
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors = ButtonDefaults.buttonColors(containerColor = LingoCoral, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.testTag("confirm_delete_account_button")
      ) {
        Text("Delete Permanently", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8), maxLines = 1, softWrap = false)
      }
    }
  )
}
