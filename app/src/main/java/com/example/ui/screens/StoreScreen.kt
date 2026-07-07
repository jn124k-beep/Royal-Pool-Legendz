package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

data class StorePackageItem(
    val title: String,
    val coinAmount: Int,
    val cashAmount: Int,
    val prizeTag: String,
    val costDollars: Double,
    val promoLabel: String = ""
)

@Composable
fun StoreScreen(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coinPacks = listOf(
        StorePackageItem("Starter Stack", 5000, 0, "🪙 5K", 0.99),
        StorePackageItem("Pro Purse", 20000, 0, "🪙 20K", 2.99, "Best Seller"),
        StorePackageItem("Legend Vault", 100000, 0, "🪙 100K", 9.99, "Hot Offer")
    )

    val cashPacks = listOf(
        StorePackageItem("Diamond Cache", 0, 50, "💎 50", 1.99),
        StorePackageItem("Emperor Bundle", 0, 250, "💎 250", 4.99, "Recommended"),
        StorePackageItem("Cosmic Fortune", 0, 1000, "💎 1K", 14.99, "Mega Value")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RoyalHeader(
                title = "ELITE BANK VAULT",
                coins = profile.coins,
                premium = profile.premiumCurrency,
                onBackClick = onBackClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("store_scrollable"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Elite VIP promo subscription banner
                item {
                    VipPackagePromoPanel(
                        isVip = profile.isVip,
                        onBuyVip = { viewModel.simulateVipPurchase() }
                    )
                }

                // Coins package heading
                item {
                    Text(
                        text = "🪙 GOLD COIN PACKAGES",
                        color = BrightBrass,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Coins list items
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        coinPacks.forEach { pack ->
                            StorePackageRowItem(
                                pack = pack,
                                isCash = false,
                                onClick = { viewModel.buyCoinsPackage(pack.coinAmount, pack.costDollars) }
                            )
                        }
                    }
                }

                // Cash Packages heading
                item {
                    Text(
                        text = "💎 PREMIUM CASH PACKAGES",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Cash list items
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cashPacks.forEach { pack ->
                            StorePackageRowItem(
                                pack = pack,
                                isCash = true,
                                onClick = { viewModel.buyPremiumPackage(pack.cashAmount, pack.costDollars) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VipPackagePromoPanel(
    isVip: Boolean,
    onBuyVip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2E2413), Color(0xFF130E07))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgGradient)
            .border(2.dp, BrightBrass, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚜️ VIP MEMBERSHIP CLUB",
                    color = BrightBrass,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                if (isVip) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F5132))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "ACTIVE", color = Color(0xFF00FF88), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Activate VIP to immediately claim +10,000 Coins, 150 Premium Cash, permanent custom golden visual headers, and priority match priority cues.",
                color = ChampagneText,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!isVip) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrightBrass)
                        .clickable { onBuyVip() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SUBSCRIBE • $4.99 / MONTH",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2200FF88))
                        .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIP BENEFITS ENHANCED",
                        color = Color(0xFF00FF88),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StorePackageRowItem(
    pack: StorePackageItem,
    isCash: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = if (isCash) NeonCyan else BrightBrass

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCash) "💎" else "🪙",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pack.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (pack.promoLabel.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = pack.promoLabel, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    text = "Instantly claims ${pack.prizeTag}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, themeColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$${pack.costDollars}",
                color = themeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
