package com.notifbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.absoluteValue

/**
 * Circular app icon. Falls back to a coloured initial when the source app isn't
 * installed (or its icon can't be loaded), so every row still reads cleanly.
 */
@Composable
fun AppAvatar(packageName: String, appLabel: String, size: Dp = 42.dp) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(avatarColor(appLabel)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val AVATAR_PALETTE = listOf(
    0xFF5C6BC0, 0xFF26A69A, 0xFFEC407A, 0xFFAB47BC,
    0xFFFFA726, 0xFF66BB6A, 0xFF42A5F5, 0xFF8D6E63,
)

private fun avatarColor(label: String): Color {
    val idx = label.hashCode().absoluteValue % AVATAR_PALETTE.size
    return Color(AVATAR_PALETTE[idx])
}
