package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Processes a user-selected image [Uri] from Android Photo Picker into a center-cropped
 * square JPEG Data URI (`data:image/jpeg;base64,...`) suitable for fast rendering and profile storage.
 */
fun processPickedProfileImage(context: Context, uri: Uri, targetSizePx: Int = 256): String? {
  return try {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOptions)
    }
    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

    var sampleSize = 1
    while (
      boundsOptions.outWidth / (sampleSize * 2) >= targetSizePx &&
      boundsOptions.outHeight / (sampleSize * 2) >= targetSizePx
    ) {
      sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: return null

    processCapturedProfileBitmap(rawBitmap, targetSizePx)
  } catch (_: Exception) {
    null
  }
}

/**
 * Center-crops and scales a [Bitmap] into a compact JPEG Data URI (`data:image/jpeg;base64,...`).
 */
fun processCapturedProfileBitmap(rawBitmap: Bitmap, targetSizePx: Int = 256): String? {
  return try {
    val side = min(rawBitmap.width, rawBitmap.height)
    val xOffset = (rawBitmap.width - side) / 2
    val yOffset = (rawBitmap.height - side) / 2
    val squareBitmap = Bitmap.createBitmap(rawBitmap, xOffset, yOffset, side, side)
    val scaledBitmap = if (side != targetSizePx) {
      Bitmap.createScaledBitmap(squareBitmap, targetSizePx, targetSizePx, true)
    } else {
      squareBitmap
    }

    val output = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
    val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    "data:image/jpeg;base64,$base64"
  } catch (_: Exception) {
    null
  }
}

/**
 * Decodes a `data:image/...;base64,...` string into a [Bitmap] if valid.
 */
fun decodeDataUriBitmap(dataUri: String?): Bitmap? {
  if (dataUri.isNullOrBlank() || !dataUri.startsWith("data:image")) return null
  return try {
    val commaIndex = dataUri.indexOf(',')
    if (commaIndex == -1) return null
    val base64Part = dataUri.substring(commaIndex + 1)
    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  } catch (_: Exception) {
    null
  }
}

@Composable
fun LingoAvatar(
  name: String,
  userId: String,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  customImageUrl: String? = null,
  countryFlag: String? = null,
  isOnline: Boolean = false,
  isPro: Boolean = false,
  borderWidth: Dp = 1.5.dp,
  showCameraBadge: Boolean = false,
  onClick: (() -> Unit)? = null
) {
  val cleanImageUrl = customImageUrl?.trim()?.takeIf { it.isNotBlank() }
  val decodedBitmap by androidx.compose.runtime.produceState<Bitmap?>(initialValue = null, cleanImageUrl) {
    value = if (cleanImageUrl != null && cleanImageUrl.startsWith("data:image")) {
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        decodeDataUriBitmap(cleanImageUrl)
      }
    } else {
      null
    }
  }
  val initial = remember(name) {
    name.trim().take(1).uppercase()
  }

  val borderBrush = Brush.linearGradient(
    colors = if (isPro) {
      listOf(LingoGold, Color(0xFFF59E0B))
    } else {
      listOf(LingoCyan, LingoElectricBlue)
    }
  )

  val backgroundBrush = Brush.linearGradient(
    colors = listOf(LingoElectricBlue, LingoCyan)
  )

  Box(
    modifier = modifier
      .size(size)
      .then(
        if (onClick != null) {
          Modifier
            .clip(CircleShape)
            .clickable { onClick() }
        } else {
          Modifier
        }
      )
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(borderWidth, borderBrush, CircleShape)
        .background(backgroundBrush),
      contentAlignment = Alignment.Center
    ) {
      when {
        decodedBitmap != null -> {
          decodedBitmap?.let { bmp ->
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "$name's profile photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(size)
                .clip(CircleShape)
            )
          }
        }
        cleanImageUrl != null && !cleanImageUrl.startsWith("data:image") -> {
          // Fallback initial behind AsyncImage in case remote URL is loading
          if (initial.isNotBlank()) {
            Text(
              text = initial,
              fontSize = (size.value * 0.40f).sp,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold
            )
          } else {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Profile",
              tint = Color.White,
              modifier = Modifier.size(size * 0.55f)
            )
          }
          AsyncImage(
            model = cleanImageUrl,
            contentDescription = "$name's profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(size)
              .clip(CircleShape)
          )
        }
        initial.isNotBlank() -> {
          Text(
            text = initial,
            fontSize = (size.value * 0.40f).sp,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
          )
        }
        else -> {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
          )
        }
      }
    }

    // Country Flag Badge
    if (!countryFlag.isNullOrBlank()) {
      val badgeSize = (size.value * 0.38f).coerceAtLeast(14f).dp
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .offset(x = (-2).dp, y = 2.dp)
          .size(badgeSize)
          .clip(CircleShape)
          .background(Color(0xFF040A1A))
          .border(1.dp, Color(0xFF1E2E62), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = countryFlag,
          fontSize = (badgeSize.value * 0.65f).sp
        )
      }
    }

    // Camera Upload Badge (for editable user profile avatar)
    if (showCameraBadge) {
      val camBadgeSize = (size.value * 0.36f).coerceIn(20f, 28f).dp
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(camBadgeSize)
          .clip(CircleShape)
          .background(LingoCyan)
          .border(1.5.dp, Color(0xFF040A1A), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PhotoCamera,
          contentDescription = "Upload profile photo",
          tint = Color(0xFF040B1A),
          modifier = Modifier.size(camBadgeSize * 0.6f)
        )
      }
    } else if (isOnline) {
      // Online Status Indicator
      val dotSize = (size.value * 0.28f).coerceIn(8f, 14f).dp
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(dotSize)
          .clip(CircleShape)
          .background(LingoEmerald)
          .border(1.5.dp, Color(0xFF040A1A), CircleShape)
      )
    } else if (isPro) {
      val starSize = (size.value * 0.32f).coerceIn(10f, 16f).dp
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(starSize)
          .clip(CircleShape)
          .background(LingoGold)
          .border(1.dp, Color.Black, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Pro",
          tint = Color.Black,
          modifier = Modifier.size(starSize * 0.7f)
        )
      }
    }
  }
}
