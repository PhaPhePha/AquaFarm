package com.phahoang.aquafarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phahoang.aquafarm.data.model.Player
import com.phahoang.aquafarm.ui.components.theme.*

@Composable
fun PlayerStatusBar(player: Player, modifier: Modifier = Modifier) {
    val expProgress = player.expProgress().coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Level ──
            Text(
                "Lv.${player.level}",
                color = Gold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(10.dp))

            // ── EXP Bar ──
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = expProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Gold,
                    trackColor = BarBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${player.currentExp}/${player.expToNextLevel()}",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            // ── Coins ──
            Text(
                "💰 ${formatCoins(player.coins)}",
                color = Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCoins(coins: Long): String {
    return when {
        coins >= 1_000_000 -> String.format("%.1fM", coins / 1_000_000.0)
        coins >= 1_000 -> String.format("%.1fK", coins / 1_000.0)
        else -> "$coins"
    }
}