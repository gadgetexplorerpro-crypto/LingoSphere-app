package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Indigo -> Cyan Gradient Endpoints (#6C63FF -> #22D3EE)
val LingoBrandIndigo = Color(0xFF6C63FF)
val LingoBrandCyan = Color(0xFF22D3EE)

// Primary Brand Accents (kept compatible with existing references)
val LingoElectricBlue = Color(0xFF6C63FF)
val LingoElectricBlueLight = Color(0xFF6C63FF)
val LingoCyan = Color(0xFF22D3EE)
val LingoCyanSoft = Color(0xFFE0F7FE)

// Secondary Indigo/Violet Accents
val LingoViolet = Color(0xFF6C63FF)
val LingoVioletLight = Color(0xFF8B84FF)
val LingoLilac = Color(0xFFA8B3FF)
val LingoVioletSoft = Color(0xFFEEF0FF)

// Premium Dark Surfaces (#070B14 canvas / #121826 / #182033)
val LingoNavyDeep = Color(0xFF070B14)
val LingoNavySurface = Color(0xFF121826)
val LingoNavyCard = Color(0xFF182033)
val LingoNavyStroke = Color(0x1AFFFFFF) // 1px white at 10% inner highlight

// Light Theme Surfaces
val LingoLightBg = Color(0xFFF6F8FC)
val LingoLightSurface = Color(0xFFFFFFFF)
val LingoLightCard = Color(0xFFEEF2F8)
val LingoLightStroke = Color(0xFFE2E8F0)

// Text Tokens (WCAG >= 4.5:1)
val LingoTextPrimaryDark = Color(0xFF0B0F1A)
val LingoTextSecondaryDark = Color(0xFF475569)
val LingoTextPrimaryLight = Color(0xFFFFFFFF)
val LingoTextSecondaryLight = Color(0xFFA8B3CF)

// Status & Gold Tokens (Gold #F5C451 strictly for Pro locks)
val LingoGold = Color(0xFFF5C451)
val LingoAmber = Color(0xFFF5C451)
val LingoAmberSoft = Color(0xFFFEF3C7)
val LingoEmerald = Color(0xFF22C55E)
val LingoEmeraldSoft = Color(0xFFD1FAE5)
val LingoCoral = Color(0xFFEF4444)
val LingoCoralSoft = Color(0xFFFEE2E2)

object LingoGradients {
  val BrandGradient = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val HeroGradient = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val ElectricPurple = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val CyanElectric = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val PurpleViolet = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val CardBorderGradient = listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f))
  val LiveIndicatorGradient = listOf(Color(0xFF6C63FF), Color(0xFF22D3EE))
  val ProGoldGradient = listOf(Color(0xFFF5C451), Color(0xFFF5C451))
}
