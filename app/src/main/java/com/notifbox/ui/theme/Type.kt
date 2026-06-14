package com.notifbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notifbox.R

// WorkSans is shipped as a single variable font; we pin weights via FontVariation.
@OptIn(ExperimentalTextApi::class)
private fun workSans(weight: Int) = Font(
    R.font.worksans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val WorkSans = FontFamily(
    workSans(400),
    workSans(500),
    workSans(600),
    workSans(700),
)

// Scale adapted from the Best-Flutter-UI-Templates design language: bold dark
// titles, relaxed letter spacing, light body text.
val NotifTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.18.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = WorkSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp,
    ),
)
