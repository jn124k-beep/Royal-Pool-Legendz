package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.window.DialogProperties
import kotlin.random.Random
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchHistoryItem
import com.example.data.PlayerProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    onNavigateToCareer: () -> Unit,
    onNavigateToCues: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToGamePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpinWheelDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInstallPrompt by remember { mutableStateOf(false) }

    val recentMatchesState = viewModel.recentMatches.collectAsState()
    val recentMatches = recentMatchesState.value

    // Spin wheel rotation transition animation state
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        if (!viewModel.isLoggedIn) {
            AaaLoginScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header stats
                RoyalHeader(
                    title = "ROYAL POOL LOBBY",
                    coins = profile.coins,
                    premium = profile.premiumCurrency
                )

                // Dynamic Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_scrollable"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Player Profile Stats Card
                    item {
                        PlayerLobbyProfileCard(
                            profile = profile,
                            onSpinClick = { showSpinWheelDialog = true },
                            onNavigateToLeaderboard = onNavigateToLeaderboard
                        )
                    }

                    // Large Central Play Now & Multiplayer Lobby Panel
                    item {
                        MatchmakingLobbyCard(
                            viewModel = viewModel,
                            profile = profile,
                            onStartGame = onNavigateToGamePlay
                        )
                    }

                    // Game Modes Navigation Hub (Career, Cues, Shop)
                    item {
                        GameModesHub(
                            viewModel = viewModel,
                            onNavigateToCareer = onNavigateToCareer,
                            onNavigateToStore = onNavigateToStore,
                            onNavigateToCues = onNavigateToCues
                        )
                    }

                    // AAA Premium Utility Hub (Settings, Rewards, Install Companion)
                    item {
                        LobbyUtilityControlsHub(
                            viewModel = viewModel,
                            onSpinClick = { showSpinWheelDialog = true },
                            onSettingsClick = { showSettingsDialog = true },
                            onInstallClick = { showInstallPrompt = true }
                        )
                    }

                    // VIP Elite Membership Promo
                    if (!profile.isVip) {
                        item {
                            VipPromoCard(onActivateVip = { viewModel.simulateVipPurchase() })
                        }
                    }

                    // Recent Match History Logger
                    item {
                        Text(
                            text = "🏆 CHAMPIONSHIP GAME LOGS",
                            color = BrightBrass,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (recentMatches.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No matches registered yet. Start a Career or Online game to log history!",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            }
                        }
                    } else {
                        items(recentMatches) { match ->
                            MatchHistoryRowItem(match = match)
                        }
                    }
                }
            }
        }

        // Active Spin Wheel Dialog Modal Overlay
        if (showSpinWheelDialog) {
            SpinWheelDialog(
                viewModel = viewModel,
                glowAlpha = glowAlpha,
                onDismiss = { showSpinWheelDialog = false }
            )
        }

        // Advanced Customizable Settings Dialog Modal
        if (showSettingsDialog) {
            AdvancedSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Progressive App Installation Prompt Modal
        if (showInstallPrompt) {
            InstallAppPromptDialog(
                onInstall = {
                    viewModel.installApp()
                    showInstallPrompt = false
                },
                onDismiss = { showInstallPrompt = false }
            )
        }

        // Onboarding Interactive Beginner Tutorial Overlay
        if (viewModel.isLoggedIn && viewModel.tutorialStep in 1..5) {
            InteractiveTutorialOverlay(viewModel = viewModel)
        }
    }
}

@Composable
fun PlayerLobbyProfileCard(
    profile: PlayerProfile,
    onSpinClick: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpNeeded = profile.level * 1000
    val progress = (profile.xp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    // Player Title Level Decider
    val levelTitle = when {
        profile.level >= 100 -> "Pool Legend"
        profile.level >= 50 -> "Grandmaster"
        profile.level >= 20 -> "Elite Pro"
        profile.level >= 5 -> "Amateur Champ"
        else -> "Pool Novice"
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_profile_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shiny visual Avatar with Crown if VIP
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (profile.isVip) listOf(Color(0xFFFFF099), Color(0xFFD4AF37))
                            else listOf(Color(0xFF2D3748), Color(0xFF1A202C))
                        )
                    )
                    .border(
                        2.dp,
                        if (profile.isVip) BrightBrass else Color.Gray,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (profile.isVip) "👑" else "🎱",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("player_name")
                    )
                    if (profile.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrightBrass)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "VIP",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Text(
                    text = "$levelTitle • Level ${profile.level}",
                    color = BrightBrass,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("player_title_level")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // XP Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A3446))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(RoyalGold, BrightBrass)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.xp} / ${xpNeeded} XP",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Divider(
            color = CardBorder.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onNavigateToLeaderboard() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔥 Rank Rating: ", color = TextMuted, fontSize = 12.sp)
                Text(
                    text = "${profile.rankPoints} RP",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("player_ranking_points")
                )
            }

            // Lucky Spin button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5252), Color(0xFFFF7B00))
                        )
                    )
                    .clickable { onSpinClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🎡 Lucky Spin",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun MatchmakingLobbyCard(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchmakingActive = viewModel.matchmakingActive
    val progress = viewModel.matchmakingProgress

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("matchmaking_card"),
        borderColor = if (matchmakingActive) NeonCyan else CardBorder,
        backgroundColor = if (matchmakingActive) Color(0xFF0D1B2A) else CardSurface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!matchmakingActive) {
                Text(
                    text = "REAL-TIME 1v1 CHAMPIONSHIP",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Compete with global legends. Entry: 250 Coins • Match stakes apply.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                GoldenButton(
                    text = "Queue Matchmaking",
                    onClick = {
                        if (profile.coins >= 250) {
                            viewModel.triggerOnlineMatchmaking(onSuccess = onStartGame)
                        } else {
                            viewModel.showMessage("Insufficient Coins! Need 250 Coins to play 1v1.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tag = "queue_match_btn"
                )
            } else {
                Text(
                    text = "SEARCHING WORLDWIDE LEGENDS...",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Searching Indicator Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Syncing server rating: ${profile.rankPoints} RP...",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun GameModesHub(
    viewModel: GameViewModel,
    onNavigateToCareer: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToCues: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Career Mode Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1B221E))
                .border(1.dp, EmeraldFelt.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .clickable { onNavigateToCareer() }
                .padding(14.dp)
        ) {
            Text(text = "🗺️", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Career Mode",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "5000+ challenges",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        // Cues Collection Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF261D13))
                .border(1.dp, RoyalGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { onNavigateToCues() }
                .padding(14.dp)
        ) {
            Text(text = "🥖", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Cues Vault",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Premium collection",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        // Shop Mode Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF211326))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { onNavigateToStore() }
                .padding(14.dp)
        ) {
            Text(text = "🪙", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Gold Store",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coins & Bundles",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun VipPromoCard(
    onActivateVip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2A2110), Color(0xFF130E07))
                )
            )
            .border(1.5.dp, BrightBrass, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚜️ VIP ROYAL CLUB",
                    color = BrightBrass,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrightBrass)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HOT",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Join Elite Pool Masters for +10,000 Coins daily, exclusive diamond-textured cues, and gold championship tables.",
                color = ChampagneText,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrightBrass)
                    .clickable { onActivateVip() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "UNLEASH ELITE MEMBERSHIP",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun MatchHistoryRowItem(
    match: MatchHistoryItem,
    modifier: Modifier = Modifier
) {
    val resultColor = if (match.playerWon) Color(0xFF00FF88) else Color(0xFFFF5252)

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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (match.playerWon) Color(0x2200FF88) else Color(0x22FF5252)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (match.playerWon) "🏆" else "💀",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = match.opponentName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${match.mode} • Rating ${match.opponentRating}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (match.playerWon) "WIN" else "LOSS",
                color = resultColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (match.coinsEarned >= 0) "+${match.coinsEarned} 🪙" else "${match.coinsEarned} 🪙",
                color = if (match.coinsEarned >= 0) Color(0xFFFFD700) else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpinWheelDialog(
    viewModel: GameViewModel,
    glowAlpha: Float,
    onDismiss: () -> Unit
) {
    val isSpinning = viewModel.isLuckySpinRolling
    val spinResult = viewModel.luckySpinResult

    // Rotation angle state for continuous visual spin
    var spinAngle by remember { mutableStateOf(0f) }
    val animatedAngle by animateFloatAsState(
        targetValue = spinAngle,
        animationSpec = tween(
            durationMillis = 2000,
            easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f) // smooth deceleration
        ),
        label = "wheelRotation"
    )

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isSpinning, dismissOnClickOutside = !isSpinning),
        confirmButton = {},
        dismissButton = {},
        containerColor = Color(0xFF0F1319),
        modifier = Modifier.border(2.dp, RoyalGold, RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "🔮 LUCKY REWARDS WHEEL",
                color = BrightBrass,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spin daily to win thousands of Coins and special legendary accessories!",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Wheel graphic Canvas
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .rotate(animatedAngle)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .border(4.dp, BrightBrass, CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val itemsCount = 6
                        val sweepAngle = 360f / itemsCount
                        val colors = listOf(
                            Color(0xFFFF5252), Color(0xFFD4AF37),
                            Color(0xFF3B82F6), Color(0xFF10B981),
                            Color(0xFF8B5CF6), Color(0xFFF59E0B)
                        )
                        val prizeLabels = listOf("100", "500", "💎 2", "VIP", "⭐", "1000")

                        for (i in 0 until itemsCount) {
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(colors[i % colors.size], colors[i % colors.size].copy(alpha = 0.7f))
                                ),
                                startAngle = i * sweepAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                        }

                        // Draw simple inner gold coin pin
                        drawCircle(
                            color = BrightBrass,
                            radius = 24f,
                            center = center
                        )
                    }
                }

                // Selector pointer indicator
                Text(
                    text = "▼",
                    color = BrightBrass,
                    fontSize = 20.sp,
                    modifier = Modifier.offset(y = (-14).dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Spin results text
                if (spinResult != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3300FF88))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎁 Sunk Prize: $spinResult!",
                            color = Color(0xFF00FF88),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                GoldenButton(
                    text = if (isSpinning) "Spinning..." else "Spin Wheel",
                    onClick = {
                        if (!isSpinning) {
                            // Increment wheel target angle by full rotations + random slice
                            spinAngle += 1080f + Random.nextInt(360)
                            viewModel.rollLuckySpinWheel()
                        }
                    },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isSpinning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Return to Lobby",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

// ==========================================
// AAA Cinematic Login & Register Screen
// ==========================================
@Composable
fun AaaLoginScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var musicEnabled by remember { mutableStateOf(true) }

    // Pulsing logo animation
    val infiniteTransition = rememberInfiniteTransition()
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF142416), Color(0xFF070B0D)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic Ambient felt fibers canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fiberCount = 40
            val random = java.util.Random(101)
            for (i in 0 until fiberCount) {
                drawCircle(
                    color = Color(0x0600FF88),
                    radius = random.nextFloat() * 200f + 100f,
                    center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Music State indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x11FFFFFF))
                        .clickable { musicEnabled = !musicEnabled }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (musicEnabled) "🎵 Music: ON" else "🔇 Music: OFF",
                        color = if (musicEnabled) BrightBrass else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulsing Game Logo / Crest
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .border(2.dp, RoyalGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎱",
                    fontSize = 54.sp
                )
            }

            Text(
                text = "ROYAL POOL\nLEGENDS",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                lineHeight = 34.sp
            )

            Text(
                text = "The Ultimate 3D Billiards Experience",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cinematic Glass login sheet
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "CREATE CHAMPIONSHIP PROFILE" else "SIGN IN TO YOUR CLUB",
                        color = BrightBrass,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Email Field
                    Column {
                        Text(text = "CLUB EMAIL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1318))
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .testTag("login_email_input")
                        )
                    }

                    // Password Field
                    Column {
                        Text(text = "SECURITY ACCESS PIN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1318))
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .testTag("login_password_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action button
                    GoldenButton(
                        text = if (isRegisterMode) "REGISTER PROFILE" else "ENTER CLUB LOBBY",
                        onClick = { viewModel.simulateLogin("Email") },
                        modifier = Modifier.fillMaxWidth().testTag("login_submit_button")
                    )

                    // Alternate mode trigger
                    TextButton(
                        onClick = { isRegisterMode = !isRegisterMode },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have a Cue ID? Sign In" else "Create a free Champion Account",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick alternative providers divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
                Text(
                    text = " OR JOIN INSTANTLY WITH ",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
            }

            // Google & Guest alternative logins Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google Login
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.simulateLogin("Google") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🔴", fontSize = 12.sp) // Simulated Google Logo
                        Text(text = "Google Account", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Guest Login
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.simulateLogin("Guest") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "👤", fontSize = 12.sp)
                        Text(text = "Play as Guest", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// Lobby Utility controls panel grid
// ==========================================
@Composable
fun LobbyUtilityControlsHub(
    viewModel: GameViewModel,
    onSpinClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "⚙️ CHAMPION UTILITIES",
            color = BrightBrass,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Settings Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .clickable { onSettingsClick() }
                    .padding(14.dp)
                    .testTag("utility_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚙️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Settings", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Lucky Spin Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .clickable { onSpinClick() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎁", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Lucky Spin", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Install Companion App Button
            val isInstalled = viewModel.isAppInstalled
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isInstalled) Color(0x2210B981) else CardSurface)
                    .border(1.dp, if (isInstalled) Color(0xFF10B981) else CardBorder, RoundedCornerShape(14.dp))
                    .clickable { onInstallClick() }
                    .padding(14.dp)
                    .testTag("utility_install_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isInstalled) "✅" else "📲", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isInstalled) "Installed" else "Install App",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// Advanced Settings Dialog modal
// ==========================================
@Composable
fun AdvancedSettingsDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    var sensitivity by remember { mutableStateOf(viewModel.aimSensitivity) }
    var powerSens by remember { mutableStateOf(viewModel.powerSensitivity) }
    var cueSpeed by remember { mutableStateOf(viewModel.cueMovementSpeed) }
    var handedness by remember { mutableStateOf(viewModel.isLeftHandedMode) }
    var vibration by remember { mutableStateOf(viewModel.isVibrationEnabled) }
    var assist by remember { mutableStateOf(viewModel.isAimAssistEnabled) }
    var graphics by remember { mutableStateOf(viewModel.graphicsQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1216),
        modifier = Modifier.border(2.dp, RoyalGold, RoundedCornerShape(24.dp)),
        confirmButton = {
            GoldenButton(
                text = "Apply Settings",
                onClick = {
                    viewModel.aimSensitivity = sensitivity
                    viewModel.powerSensitivity = powerSens
                    viewModel.cueMovementSpeed = cueSpeed
                    viewModel.isLeftHandedMode = handedness
                    viewModel.isVibrationEnabled = vibration
                    viewModel.isAimAssistEnabled = assist
                    viewModel.graphicsQuality = graphics
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        title = {
            Text(
                text = "⚙️ CLUB CONTROLS & SETTINGS",
                color = BrightBrass,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure advanced touch gestures, shooting physics deflections, and visual parameters.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Aim Sensitivity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🎯 Aim Sensitivity", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%.2fx".format(sensitivity), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(activeTrackColor = NeonCyan)
                    )
                }

                // 2. Power Sensitivity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "⚡ Power Scale Slider Sensitivity", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%.2fx".format(powerSens), color = RoyalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = powerSens,
                        onValueChange = { powerSens = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(activeTrackColor = RoyalGold)
                    )
                }

                // 3. Cue Movement Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🏑 Cue Rotation Dampening Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%.2fx".format(cueSpeed), color = BrightBrass, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = cueSpeed,
                        onValueChange = { cueSpeed = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(activeTrackColor = BrightBrass)
                    )
                }

                Divider(color = CardBorder, thickness = 1.dp)

                // Toggles Row (Handedness, Vibration, Aim Assist)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Left handed Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Left-Handed Layout", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Swaps shooting slider sides", color = TextMuted, fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (handedness) Color(0x3300FF88) else Color(0x11FFFFFF))
                                .border(1.dp, if (handedness) Color(0xFF00FF88) else Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { handedness = !handedness }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = if (handedness) "ACTIVE" else "DISABLED", color = if (handedness) Color(0xFF00FF88) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Table Vibration / Shake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Impact Table Vibration", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Triggers realistic camera shaking on hit", color = TextMuted, fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (vibration) Color(0x3300FF88) else Color(0x11FFFFFF))
                                .border(1.dp, if (vibration) Color(0xFF00FF88) else Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { vibration = !vibration }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = if (vibration) "ON" else "OFF", color = if (vibration) Color(0xFF00FF88) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Beginner Aim Assist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Beginner Aim Assist", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Toggles deflection paths & targeting line", color = TextMuted, fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (assist) Color(0x3300FF88) else Color(0x11FFFFFF))
                                .border(1.dp, if (assist) Color(0xFF00FF88) else Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { assist = !assist }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("toggle_aim_assist")
                        ) {
                            Text(text = if (assist) "ENABLED" else "DISABLED", color = if (assist) Color(0xFF00FF88) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Divider(color = CardBorder, thickness = 1.dp)

                // Graphics quality selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "🖥️ Graphics Quality Preset", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High", "Ultra 60FPS").forEach { mode ->
                            val isSel = graphics == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RoyalGold else CardSurface)
                                    .border(1.dp, if (isSel) BrightBrass else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { graphics = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// PWA Progressive app installation prompt
// ==========================================
@Composable
fun InstallAppPromptDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF090D11),
        modifier = Modifier.border(2.dp, BrightBrass, RoundedCornerShape(24.dp)),
        confirmButton = {
            GoldenButton(
                text = "✨ Install Native Shortcut",
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Later", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "📲 INSTALL ROYAL POOL LEGENDS",
                color = BrightBrass,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🎱", fontSize = 48.sp)
                Text(
                    text = "Enjoy an offline-first native performance upgrade, quick home screen launches, cinematic visual rendering, and zero delay touch controls!",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// ==========================================
// Onboarding Interactive Beginner Tutorial
// ==========================================
@Composable
fun InteractiveTutorialOverlay(
    viewModel: GameViewModel
) {
    val step = viewModel.tutorialStep
    val heading = when (step) {
        1 -> "🏆 WELCOME CHAMPION"
        2 -> "🎯 TOUCH-BASED AIMING"
        3 -> "⚙️ PRECISE AIM DIAL"
        4 -> "⚡ CHARGING SHOT FORCE"
        else -> "🎱 THE GOLDEN 8-BALL RULE"
    }

    val description = when (step) {
        1 -> "Welcome to Royal Pool Legends, a cinematic 3D billiards club! Let's get you certified with standard 8-ball championship regulations in 4 quick steps."
        2 -> "TAPPING or DRAGGING directly on the pool table rotates your cue instantly to point right at your finger. Give it a try for fluid sweeps!"
        3 -> "For expert layouts, use the AIM DIAL steppers (◀ and ▶) at the bottom control deck for micro-adjustments tailored to your aim sensitivity."
        4 -> "Swipe the SHOT FORCE slider at the bottom. Pulling it back gauges power: 10% for soft safety taps, 50% for standard splits, and 100% for maximum breaks!"
        else -> "Sink your Solid or Stripe category balls first, then secure the Black 8-Ball legally to win! Fails or sinking 8-Ball early forfeits immediately."
    }

    val buttonText = if (step == 5) "FINISH & PLAY LOBBY" else "NEXT TUTORIAL STEP"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual pointer animation indicator
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200FF88))
                    .border(2.dp, Color(0xFF00FF88), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (step) {
                        1 -> "👋"
                        2 -> "🎯"
                        3 -> "⚙️"
                        4 -> "⚡"
                        else -> "🎱"
                    },
                    fontSize = 32.sp
                )
            }

            // Glass container
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "STEP $step OF 5",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = heading,
                        color = BrightBrass,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = description,
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GoldenButton(
                        text = buttonText,
                        onClick = { viewModel.completeTutorial() },
                        modifier = Modifier.fillMaxWidth().testTag("tutorial_next_button")
                    )

                    TextButton(onClick = { viewModel.skipTutorial() }) {
                        Text(text = "Skip Tutorial", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
