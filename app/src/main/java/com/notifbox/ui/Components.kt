package com.notifbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.notifbox.ui.theme.NB

/** Signature asymmetric card shape (one large rounded corner) from the reference design. */
val NbCardShape = RoundedCornerShape(
    topStart = 8.dp, topEnd = 32.dp, bottomEnd = 8.dp, bottomStart = 8.dp,
)

/** White card with the soft grey-tinted shadow used across every screen. */
@Composable
fun NbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    var m = modifier
        .fillMaxWidth()
        .shadow(elevation = 8.dp, shape = NbCardShape, clip = false, spotColor = NB.Grey, ambientColor = NB.Grey)
        .clip(NbCardShape)
        .background(MaterialTheme.colorScheme.surface)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m.padding(contentPadding)) { content() }
}
