package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.MatchState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // Sound effects generator
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            null
        }
    }

    // Play physical synthesized sounds based on event type
    val playSound: (String) -> Unit = { event ->
        coroutineScope.launch {
            try {
                when (event) {
                    "collision" -> {
                        // High-pitched short wood click
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                    }
                    "sink" -> {
                        // Sliding pocket hollow sound simulated with lower tones
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
                    }
                    "scratch" -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    // Observe active match states from viewModel
    val profile by viewModel.playerProfile.collectAsState()
    val matchState = viewModel.matchState
    val isUserTurn = viewModel.isPlayerOneTurn
    val p1Category = viewModel.playerOneCategory
    val p2Category = viewModel.playerTwoCategory
    val balls = viewModel.physicsEngine.balls

    // Spin grid dial state
    var showSpinSelector by remember { mutableStateOf(false) }
    var chatExpanded by remember { mutableStateOf(false) }

    // Table scale factor (maps physics coordinates 800x400 to standard Composable canvas coordinates)
    var canvasWidth by remember { mutableStateOf(1f) }
    var canvasHeight by remember { mutableStateOf(1f) }
    val scaleX = remember(canvasWidth) { canvasWidth / 800f }
    val scaleY = remember(canvasHeight) { canvasHeight / 400f }

    // Listen to rolling collision events to trigger sound synthesis
    LaunchedEffect(viewModel.matchState) {
        if (viewModel.matchState == MatchState.ROLLING) {
            // Monitor rolling states of physics balls and play clicking sounds on collision ticks
            var lastPocketCount = balls.count { it.isPocketed }
            while (viewModel.matchState == MatchState.ROLLING) {
                delay(40)
                // If any ball had a collision or went into a pocket, play sounds
                val currentPocketCount = balls.count { it.isPocketed }
                if (currentPocketCount > lastPocketCount) {
                    playSound("sink")
                    lastPocketCount = currentPocketCount
                }
                // Check if any balls are colliding on this frame
                for (i in balls.indices) {
                    val b1 = balls[i]
                    if (b1.isPocketed) continue
                    for (j in i + 1 until balls.size) {
                        val b2 = balls[j]
                        if (b2.isPocketed) continue
                        if (b1.pos.dist(b2.pos) < viewModel.physicsEngine.ballRadius * 2f + 1f &&
                            b1.vel.length() > 5f && b2.vel.length() > 5f) {
                            playSound("collision")
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RichDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Heads-Up Display stats panel
            ActiveMatchHudHeader(
                viewModel = viewModel,
                onForfeit = onBackClick
            )

            // Live commentary scrolling log ticker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F12))
                    .border(1.dp, CardBorder, RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "📢 COMMENTATOR: ${viewModel.commentatorLog}",
                    color = ChampagneText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("commentator_log")
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            val isShaking = viewModel.isCameraShaking && viewModel.isVibrationEnabled
            val shakeX = if (isShaking) (kotlin.random.Random.nextFloat() * 10f - 5f).dp else 0.dp
            val shakeY = if (isShaking) (kotlin.random.Random.nextFloat() * 10f - 5f).dp else 0.dp

            // The main Billiards Table visual arena
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f) // Standard 2:1 billiard table proportion
                    .padding(horizontal = 10.dp)
                    .offset(x = shakeX, y = shakeY)
                    .clip(RoundedCornerShape(8.dp))
                    .border(3.dp, DarkBronze, RoundedCornerShape(8.dp))
                    .testTag("pool_table_container")
            ) {
                val tableFeltColor = when (viewModel.selectedTableFeltName) {
                    "Emerald Green" -> EmeraldFelt
                    "Royal Blue" -> RoyalBlueFelt
                    "Burgundy Red" -> BurgundyFelt
                    "Midnight Slate" -> CharcoalFelt
                    else -> EmeraldFelt
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tableFeltColor)
                        .pointerInput(matchState, balls) {
                            if (matchState == MatchState.AIMING && isUserTurn) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val cueBall = balls.find { it.type == BallType.CUE }
                                        if (cueBall != null) {
                                            val cbX = cueBall.pos.x * (size.width / 800f)
                                            val cbY = cueBall.pos.y * (size.height / 400f)
                                            viewModel.currentAimAngle = atan2(offset.y - cbY, offset.x - cbX)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val cueBall = balls.find { it.type == BallType.CUE }
                                        if (cueBall != null) {
                                            val cbX = cueBall.pos.x * (size.width / 800f)
                                            val cbY = cueBall.pos.y * (size.height / 400f)
                                            val dragPos = change.position
                                            viewModel.currentAimAngle = atan2(dragPos.y - cbY, dragPos.x - cbX)
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    canvasWidth = size.width
                    canvasHeight = size.height

                    val engine = viewModel.physicsEngine

                    // Draw Pockets
                    engine.pockets.forEach { pocket ->
                        val px = pocket.pos.x * scaleX
                        val py = pocket.pos.y * scaleY
                        val pr = pocket.radius * scaleX

                        // Inner dark hole
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF030507), Color(0xFF10141A)),
                                center = Offset(px, py),
                                radius = pr
                            ),
                            radius = pr,
                            center = Offset(px, py)
                        )

                        // Outer metallic shiny lip
                        drawCircle(
                            color = BrightBrass.copy(alpha = 0.7f),
                            radius = pr,
                            center = Offset(px, py),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Draw Aim Guidelines (if user turn and aiming/charging)
                    if (isUserTurn && (matchState == MatchState.AIMING || matchState == MatchState.CUE_BACKSWING)) {
                        val aimResult = engine.calculateAimGuide(viewModel.currentAimAngle)

                        // Start points
                        val startX = aimResult.startPoint.x * scaleX
                        val startY = aimResult.startPoint.y * scaleY

                        if (viewModel.isAimAssistEnabled) {
                            val hitX = aimResult.hitPoint.x * scaleX
                            val hitY = aimResult.hitPoint.y * scaleY

                            // 1. Dotted direct path line
                            drawLine(
                                color = BrightBrass.copy(alpha = 0.85f),
                                start = Offset(startX, startY),
                                end = Offset(hitX, hitY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )

                            // 2. Projected Cue Ball contact target ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = engine.ballRadius * scaleX,
                                center = Offset(hitX, hitY),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // 3. Realistic Aiming Cursor Crosshair at Hit Point
                            drawCircle(
                                color = NeonCyan,
                                radius = 7.dp.toPx(),
                                center = Offset(hitX, hitY),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawLine(
                                color = NeonCyan,
                                start = Offset(hitX - 12.dp.toPx(), hitY),
                                end = Offset(hitX + 12.dp.toPx(), hitY),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = NeonCyan,
                                start = Offset(hitX, hitY - 12.dp.toPx()),
                                end = Offset(hitX, hitY + 12.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )

                            // 4. Draw deflection arrows if we hit an object ball
                            if (aimResult.hitBall) {
                                val cueEnd = Offset(aimResult.cueBallPathEnd.x * scaleX, aimResult.cueBallPathEnd.y * scaleY)
                                val objEnd = Offset(aimResult.objectBallPathEnd.x * scaleX, aimResult.objectBallPathEnd.y * scaleY)

                                // Dotted Cue ball tangent deflection arrow
                                drawLine(
                                    color = Color.White.copy(alpha = 0.6f),
                                    start = Offset(hitX, hitY),
                                    end = cueEnd,
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )

                                // Solid Object ball displacement path
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.9f),
                                    start = Offset(hitX, hitY),
                                    end = objEnd,
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        } else {
                            // Raw short line indicator for professional expert feel (no assist lines)
                            val shortEndX = startX + cos(viewModel.currentAimAngle) * 60f * scaleX
                            val shortEndY = startY + sin(viewModel.currentAimAngle) * 60f * scaleY
                            drawLine(
                                color = Color.White.copy(alpha = 0.7f),
                                start = Offset(startX, startY),
                                end = Offset(shortEndX, shortEndY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        }
                    }

                    // Draw 16 Billiard Balls
                    balls.forEach { ball ->
                        if (ball.isPocketed) return@forEach

                        val bx = ball.pos.x * scaleX
                        val by = ball.pos.y * scaleY
                        val br = engine.ballRadius * scaleX

                        // Render highly realistic glossy 3D sphere gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(ball.colorHex), Color(ball.colorHex).copy(alpha = 0.8f), Color.Black),
                                center = Offset(bx - br * 0.25f, by - br * 0.25f), // highlight offset
                                radius = br * 1.3f
                            ),
                            radius = br,
                            center = Offset(bx, by)
                        )

                        // Add shiny 3D white specular highlight glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.0f)),
                                center = Offset(bx - br * 0.35f, by - br * 0.35f),
                                radius = br * 0.45f
                            ),
                            radius = br * 0.45f,
                            center = Offset(bx - br * 0.35f, by - br * 0.35f)
                        )

                        // Draw ball number details if not the plain white cue ball
                        if (ball.type != BallType.CUE) {
                            // Draw Stripe overlays if stripe ball
                            if (ball.type == BallType.STRIPE) {
                                // Draw a thick horizontal white band across the center of ball
                                drawRect(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = Offset(bx - br * 0.7f, by - br * 0.3f),
                                    size = Size(br * 1.4f, br * 0.6f)
                                )
                            }

                            // Inner central white number dot
                            drawCircle(
                                color = Color.White,
                                radius = br * 0.42f,
                                center = Offset(bx, by)
                                // Only draw on top center
                            )

                            // Render text number
                            val textLayoutResult = textMeasurer.measure(
                                text = ball.number.toString(),
                                style = TextStyle(
                                    color = Color.Black,
                                    fontSize = (br * 0.65f).sp.value.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(bx - textLayoutResult.size.width / 2f, by - textLayoutResult.size.height / 2f)
                            )
                        }
                    }

                    // Draw Cue Stick representing hand physical retract
                    if (isUserTurn && (matchState == MatchState.AIMING || matchState == MatchState.CUE_BACKSWING)) {
                        val cueBall = balls.find { it.type == BallType.CUE }!!
                        val cbX = cueBall.pos.x * scaleX
                        val cbY = cueBall.pos.y * scaleY
                        val br = engine.ballRadius * scaleX

                        val angle = viewModel.currentAimAngle

                        // Cue sticks extends backwards opposite to shot angle
                        val stickDirX = -cos(angle)
                        val stickDirY = -sin(angle)

                        // Displace cue stick backwards based on backswing charging power value
                        val chargingGap = viewModel.cueShotPower * 50f * scaleX
                        val startGap = br * 1.3f + chargingGap

                        val startStickX = cbX + stickDirX * startGap
                        val startStickY = cbY + stickDirY * startGap

                        val stickLength = 160f * scaleX
                        val endStickX = cbX + stickDirX * (startGap + stickLength)
                        val endStickY = cbY + stickDirY * (startGap + stickLength)

                        // Draw dual-layered premium cue stick (Carbon fiber / Wood with gold base trims)
                        // Thin tip layer
                        drawLine(
                            color = ChampagneText,
                            start = Offset(startStickX, startStickY),
                            end = Offset(
                                cbX + stickDirX * (startGap + stickLength * 0.3f),
                                cbY + stickDirY * (startGap + stickLength * 0.3f)
                            ),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Heavy cue handle layer with luxury gold trims
                        drawLine(
                            brush = Brush.linearGradient(colors = listOf(RoyalGold, Color.Black)),
                            start = Offset(
                                cbX + stickDirX * (startGap + stickLength * 0.3f),
                                cbY + stickDirY * (startGap + stickLength * 0.3f)
                            ),
                            end = Offset(endStickX, endStickY),
                            strokeWidth = 5.6.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // User Turn interactives control dock panel
            if (isUserTurn && matchState != MatchState.ROLLING && matchState != MatchState.MATCH_OVER) {
                ControlDeckPanel(
                    viewModel = viewModel,
                    showSpinSelector = showSpinSelector,
                    onToggleSpin = { showSpinSelector = !showSpinSelector },
                    chatExpanded = chatExpanded,
                    onToggleChat = { chatExpanded = !chatExpanded }
                )
            } else if (!isUserTurn && matchState == MatchState.AIMING) {
                // Opponent is playing/aiming
                OpponentActiveLoader(viewModel = viewModel)
            } else if (matchState == MatchState.ROLLING) {
                // Sinks & collision rolling ticker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrightBrass)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }

        // Custom Overlay dialog for Spin target setup
        if (showSpinSelector) {
            SpinTargetSetupDialog(
                viewModel = viewModel,
                onDismiss = { showSpinSelector = false }
            )
        }

        // Custom Chat Emoji Overlay Drawer
        if (chatExpanded) {
            ChatEmojisDialog(
                viewModel = viewModel,
                onDismiss = { chatExpanded = false }
            )
        }

        // Match Over Final Summary Modal
        if (matchState == MatchState.MATCH_OVER) {
            MatchOverSummaryModal(
                viewModel = viewModel,
                onDismiss = onBackClick
            )
        }
    }
}

@Composable
fun ActiveMatchHudHeader(
    viewModel: GameViewModel,
    onForfeit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p1Category = viewModel.playerOneCategory
    val p2Category = viewModel.playerTwoCategory
    val isP1Turn = viewModel.isPlayerOneTurn

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070A0E))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player 1 "YOU" HUD
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "YOU (P1)",
                    color = if (isP1Turn) BrightBrass else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = if (isP1Turn) FontWeight.Black else FontWeight.Bold
                )
                Text(
                    text = if (p1Category == null) "Undecided" else p1Category.name + "s",
                    color = if (p1Category == BallType.SOLID) Color(0xFFFF4500) else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Middle Forfeit Button / Round info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                    .background(Color(0x11FF5252))
                    .clickable { onForfeit() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "Forfeit", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Player 2 "AI" HUD
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = viewModel.opponentName.uppercase(),
                    color = if (!isP1Turn) BrightBrass else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = if (!isP1Turn) FontWeight.Black else FontWeight.Bold
                )
                Text(
                    text = if (p2Category == null) "Undecided" else p2Category.name + "s",
                    color = if (p2Category == BallType.SOLID) Color(0xFFFF4500) else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ControlDeckPanel(
    viewModel: GameViewModel,
    showSpinSelector: Boolean,
    onToggleSpin: () -> Unit,
    chatExpanded: Boolean,
    onToggleChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var powerValue by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Spin Selector Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, RoyalGold, RoundedCornerShape(12.dp))
                    .clickable { onToggleSpin() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔴 Spin: ", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "V:${floor(viewModel.physicsEngine.cueBallVerticalSpin * 10).toInt()} H:${floor(viewModel.physicsEngine.cueBallSideSpin * 10).toInt()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Fine tuning Angle Steppers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardSurface)
                        .border(1.dp, CardBorder, CircleShape)
                        .clickable { viewModel.currentAimAngle -= 0.005f * viewModel.aimSensitivity },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◀", color = Color.White, fontSize = 11.sp)
                }

                Text(
                    text = "AIM DIAL",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardSurface)
                        .border(1.dp, CardBorder, CircleShape)
                        .clickable { viewModel.currentAimAngle += 0.005f * viewModel.aimSensitivity },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "▶", color = Color.White, fontSize = 11.sp)
                }
            }

            // Chat Emojis drawer toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardSurface)
                    .border(1.dp, RoyalGold, CircleShape)
                    .clickable { onToggleChat() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💬", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Power charge slider (pull cue back effect)
        val shotTypeLabel = when {
            powerValue <= 0.02f -> "Pull Back to Charge"
            powerValue < 0.25f -> "Soft Control Shot (10%)"
            powerValue < 0.75f -> "Normal Shot (50%)"
            else -> "🔥 Powerful Break Shot (100%)"
        }

        val powerColor = when {
            powerValue < 0.25f -> Color(0xFF00FF88)
            powerValue < 0.75f -> RoyalGold
            else -> Color(0xFFFF4500)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ SHOT FORCE: ${floor(powerValue * 100f).toInt()}%",
                    color = BrightBrass,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = shotTypeLabel,
                    color = powerColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = powerValue,
                    onValueChange = {
                        // Multiply with powerSensitivity from settings
                        val adjustedVal = (it * viewModel.powerSensitivity).coerceIn(0f, 1f)
                        powerValue = adjustedVal
                        viewModel.cueShotPower = adjustedVal
                        if (viewModel.matchState == MatchState.AIMING) {
                            viewModel.triggerCueShot()
                        }
                    },
                    onValueChangeFinished = {
                        // Strike the cue!
                        if (powerValue > 0.03f) {
                            viewModel.executeShot(powerValue)
                        } else {
                            viewModel.matchState = MatchState.AIMING
                        }
                        powerValue = 0f
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = powerColor,
                        inactiveTrackColor = Color(0xFF1E293B),
                        thumbColor = BrightBrass
                    ),
                    modifier = Modifier.weight(1f).testTag("shot_power_slider")
                )
            }
        }
    }
}

@Composable
fun OpponentActiveLoader(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = NeonCyan,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${viewModel.opponentName} is calculating target paths & spins...",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpinTargetSetupDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    var localV by remember { mutableStateOf<Float>(viewModel.physicsEngine.cueBallVerticalSpin) }
    var localH by remember { mutableStateOf<Float>(viewModel.physicsEngine.cueBallSideSpin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1319),
        confirmButton = {
            Button(
                onClick = {
                    viewModel.physicsEngine.cueBallVerticalSpin = localV
                    viewModel.physicsEngine.cueBallSideSpin = localH
                    onDismiss()
                }
            ) {
                Text(text = "Apply Spin", color = Color.Black)
            }
        },
        title = {
            Text(
                text = "🎯 CUE BALL SPIN TARGET",
                color = BrightBrass,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
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
                    text = "Apply topspin (Follow) or backspin (Draw), and left/right English side deflections.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Vertical slider (topspin/backspin)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Vertical Draw: ", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = localV,
                        onValueChange = { localV = it },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(activeTrackColor = RoyalGold)
                    )
                }

                // Horizontal slider (side english)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Side English: ", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = localH,
                        onValueChange = { localH = it },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(activeTrackColor = NeonCyan)
                    )
                }
            }
        }
    )
}

@Composable
fun ChatEmojisDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val messages = listOf("Nice shot!", "Unbelievable!", "Oops, lucky!", "Good Game!", "I am the Cue Master!", "You got this!")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1319),
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Dismiss") }
        },
        title = { Text(text = "💬 SHOUT MOCK CHAT", color = BrightBrass, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                messages.forEach { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.commentatorLog = "P1 says: \"$msg\""
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Text(text = msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = CardBorder.copy(alpha = 0.4f))
                }
            }
        }
    )
}

@Composable
fun MatchOverSummaryModal(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isP1Won = viewModel.isPlayerOneTurn // if game ended legally, whoever sunk 8 ball won, wait, ViewModel resolves victory

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C0F12),
        modifier = modifier.border(2.dp, RoyalGold, RoundedCornerShape(24.dp)),
        confirmButton = {
            GoldenButton(
                text = "Back to Lobby",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        },
        title = {
            Text(
                text = "🏆 MATCH COMPLETE",
                color = BrightBrass,
                fontSize = 18.sp,
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
                    text = viewModel.commentatorLog,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TOTAL SHOTS", color = TextMuted, fontSize = 11.sp)
                        Text(text = viewModel.shotCountInMatch.toString(), color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "RATING DELTA", color = TextMuted, fontSize = 11.sp)
                        Text(text = "+25 RP", color = Color(0xFF00FF88), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
