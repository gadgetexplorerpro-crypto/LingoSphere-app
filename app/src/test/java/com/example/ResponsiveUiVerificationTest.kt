package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.example.ui.components.Button
import com.example.ui.components.OutlinedButton
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ResponsiveCard
import com.example.ui.components.ResponsiveIconButton
import com.example.ui.components.ResponsiveText
import com.example.ui.components.ScreenContainer
import com.example.ui.components.SecondaryButton
import com.example.ui.components.TextButton
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponsiveUiVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyButtonsRenderHorizontallyAcrossSmallStandardAndLargePhoneWidths() {
        val widthsDp = listOf(320.dp, 360.dp, 393.dp, 430.dp)

        composeTestRule.setContent {
            LingoSphereTheme {
                Column {
                    widthsDp.forEach { screenWidth ->
                        val tagSuffix = screenWidth.value.toInt()
                        ResponsiveCard(
                            modifier = Modifier
                                .width(screenWidth)
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // 1. Header row with long title and trailing action button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ResponsiveText(
                                        text = "Long Multilingual Translation & Learning Header Title",
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                            .testTag("header_text_$tagSuffix"),
                                        maxLines = 2
                                    )
                                    OutlinedButton(
                                        onClick = {},
                                        modifier = Modifier.testTag("retry_btn_$tagSuffix")
                                    ) {
                                        Text("Retry")
                                    }
                                }

                                // 2. 3-button row on narrow/standard screens
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("accept_btn_$tagSuffix")
                                    ) {
                                        Text("Accept", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {},
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("decline_btn_$tagSuffix")
                                    ) {
                                        Text("Decline", maxLines = 1)
                                    }
                                    TextButton(
                                        onClick = {},
                                        modifier = Modifier.testTag("skip_btn_$tagSuffix")
                                    ) {
                                        Text("Skip", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        widthsDp.forEach { screenWidth ->
            val tagSuffix = screenWidth.value.toInt()
            listOf("retry_btn_$tagSuffix", "accept_btn_$tagSuffix", "decline_btn_$tagSuffix").forEach { tag ->
                val node = composeTestRule.onNodeWithTag(tag)
                node.assertIsDisplayed()
                val bounds = node.getUnclippedBoundsInRoot()
                // Verify button is horizontal (width >= 48dp and not a tall vertical 1-letter-per-line column)
                assertTrue(
                    "Expected $tag on ${screenWidth} screen to have width >= 48.dp, actual width=${bounds.width}, height=${bounds.height}",
                    bounds.width >= 48.dp && bounds.height <= 96.dp
                )
            }
        }
    }

    @Test
    fun verifyReusableResponsiveComponentsAndLargeFontScaleResilience() {
        composeTestRule.setContent {
            // Simulate user setting Android system font size to 1.6x (Extra Large)
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.6f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                LingoSphereTheme {
                    ScreenContainer(modifier = Modifier.width(320.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PrimaryButton(
                                text = "Start Lesson",
                                icon = Icons.Default.PlayArrow,
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("primary_action_btn")
                            )
                            SecondaryButton(
                                text = "Review Vocabulary",
                                icon = Icons.Default.Refresh,
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("secondary_action_btn")
                            )
                            ResponsiveIconButton(
                                onClick = {},
                                icon = Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                modifier = Modifier.testTag("responsive_icon_btn")
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("primary_action_btn")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(120.dp)

        composeTestRule.onNodeWithTag("secondary_action_btn")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(120.dp)

        composeTestRule.onNodeWithTag("responsive_icon_btn")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }
}
