package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.CareerLevelProgress
import com.example.data.PlayerProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameViewModel

@Composable
fun CareerLevelsScreen(
    viewModel: GameViewModel,
    profile: PlayerProfile,
    progress: List<CareerLevelProgress>,
    onBackClick: () -> Unit,
    onNavigateToGamePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLevelForPopup by remember { mutableStateOf<Int?>(null) }

    // Group levels in chunks of 3 to create a winding pathway
    val rows = remember(progress) {
        // Generate placeholder progress up to 40 levels if progress list is short
        val fullList = progress.toMutableList()
        val highestConfigured = progress.maxByOrNull { it.levelNumber }?.levelNumber ?: 0
        if (highestConfigured < 40) {
            for (i in (highestConfigured + 1)..40) {
                fullList.add(CareerLevelProgress(levelNumber = i, isCompleted = false, stars = 0))
            }
        }
        fullList.chunked(3)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RoyalHeader(
                title = "CAREER ROADMAP",
                coins = profile.coins,
                premium = profile.premiumCurrency,
                onBackClick = onBackClick
            )

            // Description Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10141B))
                    .border(1.dp, CardBorder, RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Tour: World Pool Masters Series",
                        color = BrightBrass,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "5000+ TOTAL LEVELS",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Path Scroll list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("career_levels_list"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                items(rows.size) { rowIndex ->
                    val rowItems = rows[rowIndex]
                    val isEvenRow = rowIndex % 2 == 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Order elements dynamically to create a winding serpentine S-path
                        val orderedItems = if (isEvenRow) rowItems else rowItems.reversed()

                        orderedItems.forEach { level ->
                            // Determine if level is unlocked (it's unlocked if it is level 1, or completed, or its predecessor is completed)
                            val isUnlocked = level.levelNumber == 1 || level.isCompleted ||
                                    progress.any { it.levelNumber == level.levelNumber - 1 && it.isCompleted }

                            CareerPathNode(
                                level = level,
                                isUnlocked = isUnlocked,
                                onClick = {
                                    if (isUnlocked) {
                                        selectedLevelForPopup = level.levelNumber
                                    } else {
                                        viewModel.showMessage("Level ${level.levelNumber} is Locked! Clear previous levels.")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Selected Level Detail Popup Modal
        selectedLevelForPopup?.let { levelNum ->
            val difficulty = when {
                levelNum >= 30 -> "Legend"
                levelNum >= 15 -> "Master"
                levelNum >= 5 -> "Professional"
                else -> "Beginner"
            }

            val tableFelt = when (levelNum % 5) {
                1 -> "Emerald Green"
                2 -> "Royal Blue"
                3 -> "Burgundy Red"
                4 -> "Midnight Slate"
                else -> "Championship Gold"
            }

            AlertDialog(
                onDismissRequest = { selectedLevelForPopup = null },
                containerColor = Color(0xFF0F1319),
                modifier = Modifier.border(2.dp, RoyalGold, RoundedCornerShape(24.dp)),
                confirmButton = {},
                dismissButton = {},
                title = {
                    Text(
                        text = "🎱 LEVEL $levelNum CHALLENGE",
                        color = BrightBrass,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Level Goals
                        Text(
                            text = "Primary Objective:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Clear the table and pocket the black 8-ball against opponent AI ($difficulty) under 6 direct shots.",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Match settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "TABLE FELT", color = TextMuted, fontSize = 10.sp)
                                Text(text = tableFelt, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "ENTRY FEE", color = TextMuted, fontSize = 10.sp)
                                Text(text = "100 Coins", color = RoyalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { selectedLevelForPopup = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "CLOSE", color = TextMuted, fontWeight = FontWeight.Bold)
                            }

                            GoldenButton(
                                text = "STRIKE BREAK",
                                onClick = {
                                    selectedLevelForPopup = null
                                    if (profile.coins >= 100) {
                                        viewModel.startNewMatch(
                                            mode = GameMode.CAREER,
                                            oppName = "AI_${difficulty}_${levelNum}",
                                            oppDiff = difficulty,
                                            levelNum = levelNum
                                        )
                                        onNavigateToGamePlay()
                                    } else {
                                        viewModel.showMessage("Insufficient coins! Requires 100 coins.")
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                tag = "career_start_btn"
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CareerPathNode(
    level: CareerLevelProgress,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeBg = when {
        level.isCompleted -> Brush.radialGradient(colors = listOf(Color(0xFF0F5132), Color(0xFF198754)))
        isUnlocked -> Brush.radialGradient(colors = listOf(Color(0xFF2E3440), Color(0xFF4C566A)))
        else -> Brush.radialGradient(colors = listOf(Color(0xFF1E2024), Color(0xFF0C0E10)))
    }

    val borderStrokeColor = when {
        level.isCompleted -> BrightBrass
        isUnlocked -> RoyalGold
        else -> Color.DarkGray
    }

    Column(
        modifier = modifier
            .testTag("career_node_${level.levelNumber}")
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(nodeBg)
                .border(2.dp, borderStrokeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = level.levelNumber.toString(),
                    color = if (level.isCompleted) BrightBrass else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                Text(
                    text = "🔒",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Completed Stars Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val starsToDisplay = if (level.isCompleted) 3 else 0
            for (i in 1..3) {
                Text(
                    text = if (i <= starsToDisplay) "★" else "☆",
                    color = if (i <= starsToDisplay) BrightBrass else Color.DarkGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
