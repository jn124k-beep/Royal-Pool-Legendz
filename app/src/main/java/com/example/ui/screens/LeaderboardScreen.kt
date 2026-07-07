package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.PlayerProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

data class GlobalLeaderboardProfile(
    val name: String,
    val level: Int,
    val ratingPoints: Int,
    val isVip: Boolean,
    val medalEmoji: String = ""
)

@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFeltOption by remember { mutableStateOf(profile.selectedTableFelt) }

    // Simulated top global player profiles
    val topLegends = listOf(
        GlobalLeaderboardProfile("Efren_Magician", 350, 4800, true, "🥇"),
        GlobalLeaderboardProfile("O_Sullivan_8B", 280, 4420, true, "🥈"),
        GlobalLeaderboardProfile("Cue_Slayer_VIP", 195, 3950, true, "🥉"),
        GlobalLeaderboardProfile("Dmitri_Billiard", 125, 3100, false),
        GlobalLeaderboardProfile("Elena_Strike", 110, 2980, true),
        GlobalLeaderboardProfile("Sarah_Master", 95, 2750, false),
        GlobalLeaderboardProfile("Alex_Gold_Cue", 88, 2400, true),
        GlobalLeaderboardProfile("PoolNovice_No1", 72, 2150, false)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RoyalHeader(
                title = "WORLD CHAMPIONSHIP",
                coins = profile.coins,
                premium = profile.premiumCurrency,
                onBackClick = onBackClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("leaderboards_scrollable"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile statistics card
                item {
                    HistoricStatsCard(profile = profile)
                }

                // Table customization felt selector
                item {
                    FeltCustomizationCard(
                        currentFelt = selectedFeltOption,
                        onFeltSelect = { felt ->
                            selectedFeltOption = felt
                            viewModel.updateTableFelt(felt)
                        }
                    )
                }

                // Global rankings heading
                item {
                    Text(
                        text = "🏆 GLOBAL TOP RANKINGS",
                        color = BrightBrass,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Global ranking rows
                itemsIndexed(topLegends) { index, legend ->
                    GlobalLeaderRowItem(
                        legend = legend,
                        rankNumber = index + 1
                    )
                }
            }
        }
    }
}

@Composable
fun HistoricStatsCard(
    profile: PlayerProfile,
    modifier: Modifier = Modifier
) {
    val totalMatches = profile.wins + profile.losses
    val winRate = if (totalMatches > 0) (profile.wins.toFloat() / totalMatches * 100).toInt() else 0

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("historic_stats_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚡ MY CAREER METRICS",
                color = BrightBrass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MATCHES", color = TextMuted, fontSize = 10.sp)
                    Text(text = totalMatches.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "VICTORIES", color = TextMuted, fontSize = 10.sp)
                    Text(text = profile.wins.toString(), color = Color(0xFF00FF88), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "WIN RATE", color = TextMuted, fontSize = 10.sp)
                    Text(text = "$winRate%", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "STREAK", color = TextMuted, fontSize = 10.sp)
                    Text(text = "${profile.streak} L", color = BrightBrass, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FeltCustomizationCard(
    currentFelt: String,
    onFeltSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val felts = listOf("Emerald Green", "Royal Blue", "Burgundy Red", "Midnight Slate")

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = CardBorder.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "🎨 TABLE THEME CUSTOMIZATION",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                felts.forEach { felt ->
                    val isSelected = felt == currentFelt
                    val feltColor = when (felt) {
                        "Emerald Green" -> EmeraldFelt
                        "Royal Blue" -> RoyalBlueFelt
                        "Burgundy Red" -> BurgundyFelt
                        "Midnight Slate" -> CharcoalFelt
                        else -> EmeraldFelt
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.5.dp,
                                if (isSelected) BrightBrass else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .background(CardSurface)
                            .clickable { onFeltSelect(felt) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(feltColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = felt.substringBefore(" "),
                            color = if (isSelected) BrightBrass else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalLeaderRowItem(
    legend: GlobalLeaderboardProfile,
    rankNumber: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rank Badge Medal / Number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (legend.medalEmoji.isNotEmpty()) {
                    Text(text = legend.medalEmoji, fontSize = 16.sp)
                } else {
                    Text(text = "#$rankNumber", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = legend.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (legend.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrightBrass)
                                .padding(horizontal = 4.dp, vertical = 0.5.dp)
                        ) {
                            Text(text = "VIP", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    text = "Championship Level ${legend.level}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "${legend.ratingPoints} RP",
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
