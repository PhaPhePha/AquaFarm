package com.phahoang.aquafarm.ui.components.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Blue, secondary = Green, tertiary = Gold,
    background = DeepOcean, surface = CardDark,
    onPrimary = TextWhite, onBackground = TextWhite, onSurface = TextWhite
)

@Composable
fun AquaFarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
