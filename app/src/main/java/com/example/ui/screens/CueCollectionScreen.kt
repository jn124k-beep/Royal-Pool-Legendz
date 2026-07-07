package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CueItem
import kotlin.math.floor
import com.example.data.PlayerProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@Composable
fun CueCollectionScreen(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    cues: List<CueItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCue by remember { mutableStateOf<CueItem?>(null) }

    // On first load, select the currently equipped cue
    LaunchedEffect(cues) {
        if (selectedCue == null && cues.isNotEmpty()) {
            selectedCue = cues.find { it.isEquipped } ?: cues.first()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RoyalHeader(
                title = "PREMIUM CUES VAULT",
                coins = profile.coins,
                premium = profile.premiumCurrency,
                onBackClick = onBackClick
            )

            // Split pane: top part shows details of currently selected cue, bottom part lists all cues
            selectedCue?.let { cue ->
                SelectedCueDetailsCard(
                    cue = cue,
                    profile = profile,
                    onEquip = { viewModel.equipCue(cue.id) },
                    onBuyCoins = { viewModel.buyCue(cue.id, "coins") },
                    onBuyPremium = { viewModel.buyCue(cue.id, "premium") },
                    onUpgrade = { viewModel.upgradeEquippedCue(cue.id) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "🥖 CUE STOCK CATALOGUE",
                color = BrightBrass,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Cues list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("cues_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cues) { cue ->
                    CueRowListItem(
                        cue = cue,
                        isSelected = selectedCue?.id == cue.id,
                        onClick = { selectedCue = cue }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedCueDetailsCard(
    cue: CueItem,
    profile: PlayerProfile,
    onEquip: () -> Unit,
    onBuyCoins: () -> Unit,
    onBuyPremium: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val upgradeCost = cue.upgradeLevel * 800

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("selected_cue_details"),
        borderColor = if (cue.isEquipped) BrightBrass else CardBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cue.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (cue.isEquipped) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0F5132))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(text = "EQUIPPED", color = Color(0xFF00FF88), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = "${cue.category} • Upgrade Level ${cue.upgradeLevel}",
                        color = BrightBrass,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (cue.isOwned) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .clickable { onUpgrade() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Upgrade [🪙 $upgradeCost]",
                            color = BrightBrass,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress stats
            StatProgressBar(label = "AIM ACCURACY", value = cue.accuracy, color = NeonCyan)
            Spacer(modifier = Modifier.height(10.dp))
            StatProgressBar(label = "SHOT FORCE", value = cue.power, color = Color(0xFFFF5252))
            Spacer(modifier = Modifier.height(10.dp))
            StatProgressBar(label = "SPIN DEFLECTION", value = cue.spin, color = BrightBrass)

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buy or Equip Button
            if (cue.isOwned) {
                if (!cue.isEquipped) {
                    GoldenButton(
                        text = "EQUIP CUESTICK",
                        onClick = onEquip,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "CUE READY FOR BREAK", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (cue.costCoins > 0) {
                        Button(
                            onClick = onBuyCoins,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "🪙 ${String.format("%,d", cue.costCoins)}", color = BrightBrass, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (cue.costPremium > 0) {
                        Button(
                            onClick = onBuyPremium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300F5FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "💎 ${cue.costPremium}", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CueRowListItem(
    cue: CueItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = when (cue.category) {
        "Legendary" -> Color(0xFFE9D5FF)
        "Diamond Edition" -> NeonCyan
        "Gold Edition" -> BrightBrass
        else -> Color.White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1E242E) else CardSurface)
            .border(
                1.5.dp,
                if (isSelected) BrightBrass else CardBorder.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF2A2D35), Color(0xFF121418))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (cue.category) {
                        "Legendary" -> "🔥"
                        "Diamond Edition" -> "💎"
                        "Gold Edition" -> "⚜️"
                        else -> "🥖"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = cue.name,
                    color = tierColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cue.category} • Power ${floor(cue.power * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (cue.isOwned) {
                Text(
                    text = if (cue.isEquipped) "EQUIPPED" else "OWNED",
                    color = if (cue.isEquipped) Color(0xFF00FF88) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "LOCKED",
                    color = Color(0xFFFF5252),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
