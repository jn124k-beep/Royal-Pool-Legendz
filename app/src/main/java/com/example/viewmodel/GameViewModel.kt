package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.physics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.random.Random

enum class GameMode {
    FREE_PLAY, CAREER, ONLINE_1V1, TOURNAMENT
}

enum class MatchState {
    NOT_STARTED,
    INTRO_CAMERA,
    AIMING,
    CUE_BACKSWING,
    ROLLING,
    FOUL_CUE_PLACEMENT,
    MATCH_OVER
}

data class TournamentStage(
    val stageName: String, // "Quarter-Finals", "Semi-Finals", "Grand Finals"
    val opponentName: String,
    val difficulty: String, // "Beginner", "Professional", "Master", "Legend"
    val prizeCoins: Int,
    val entryFee: Int
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = GameRepository(db.gameDao())

    // Observable states from Room DB
    val playerProfile: StateFlow<PlayerProfile?> = repository.playerProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allCues: StateFlow<List<CueItem>> = repository.allCues.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val careerProgress: StateFlow<List<CareerLevelProgress>> = repository.careerProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentMatches: StateFlow<List<MatchHistoryItem>> = repository.recentMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedLevelsCount: StateFlow<Int> = repository.completedLevelsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Active Game Variables
    var activeGameMode by mutableStateOf(GameMode.FREE_PLAY)
    var matchState by mutableStateOf(MatchState.NOT_STARTED)
    val physicsEngine = BilliardsPhysicsEngine()

    // Customizable Control Settings
    var aimSensitivity by mutableStateOf(1.0f)
    var powerSensitivity by mutableStateOf(1.0f)
    var cueMovementSpeed by mutableStateOf(1.0f)
    var isLeftHandedMode by mutableStateOf(false)
    var isVibrationEnabled by mutableStateOf(true)
    var isAimAssistEnabled by mutableStateOf(true)
    var graphicsQuality by mutableStateOf("High")

    // Auth & Onboarding States
    var isLoggedIn by mutableStateOf(false)
    var loginMethod by mutableStateOf("")
    var tutorialStep by mutableStateOf(0) // 0 = inactive, 1..5 = steps
    var hasCompletedTutorial by mutableStateOf(false)

    // Install App State
    var isAppInstalled by mutableStateOf(false)
    var showInstallPrompt by mutableStateOf(false)

    // Visual/Atmospheric effects
    var isCameraShaking by mutableStateOf(false)
    var isSlowMoActive by mutableStateOf(false)

    fun simulateLogin(method: String) {
        loginMethod = method
        isLoggedIn = true
        showMessage("Logged in via $method!")
        if (!hasCompletedTutorial) {
            tutorialStep = 1
        }
    }

    fun completeTutorial() {
        hasCompletedTutorial = true
        tutorialStep = 0
        showMessage("Onboarding Complete! Starter Bonus Awarded.")
        viewModelScope.launch {
            repository.addRewards(500, 5, 200)
        }
    }

    fun skipTutorial() {
        hasCompletedTutorial = true
        tutorialStep = 0
        showMessage("Tutorial Skipped! Welcome to the Lobby.")
    }

    fun installApp() {
        isAppInstalled = true
        showMessage("App installed to your device launcher successfully!")
    }

    // Cue stick control variables
    var currentAimAngle by mutableStateOf(-PI.toFloat() / 2f) // Pointing left initially
    var cueShotPower by mutableStateOf(0f)
    var isCueBackswingActive by mutableStateOf(false)
    var selectedTableFeltName by mutableStateOf("Emerald Green")

    // Active Match Stats
    var isPlayerOneTurn by mutableStateOf(true) // User is Player 1, AI/Online is Player 2
    var playerOneCategory by mutableStateOf<BallType?>(null) // Solid, Stripe, or Undecided (null)
    var playerTwoCategory by mutableStateOf<BallType?>(null)
    var shotCountInMatch by mutableStateOf(0)
    var ballsPocketedThisTurn = mutableListOf<BilliardBall>()
    var firstBallTouchedThisTurn: BilliardBall? = null
    var isScratchCommitted by mutableStateOf(false)
    var activeFoulMessage by mutableStateOf<String?>(null)

    // AI & Online Opponent Config
    var opponentName by mutableStateOf("AI Opponent")
    var opponentDifficulty by mutableStateOf("Beginner") // Beginner, Professional, Master, Legend
    var isOpponentThinking by mutableStateOf(false)
    var commentatorLog by mutableStateOf("Welcome to the Royal Billiards Arena. Break the rack to begin!")

    // Online matchmaking simulation
    var matchmakingActive by mutableStateOf(false)
    var matchmakingProgress by mutableStateOf(0f)

    // Daily reward, chest, spin wheel states
    var luckySpinResult by mutableStateOf<String?>(null)
    var isLuckySpinRolling by mutableStateOf(false)
    var activeLevelNumber by mutableStateOf(1)

    // Tournament configuration
    var currentTournamentIndex by mutableStateOf(0)
    val tournaments = listOf(
        TournamentStage("Quarter-Finals", "Leo_The_Hawk", "Professional", 2500, 500),
        TournamentStage("Semi-Finals", "Sophia_Royal", "Master", 5000, 1000),
        TournamentStage("Grand Finals", "Vincenzo_Legend", "Legend", 15000, 3000)
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNecessary()
            // Observe selected felt color from profile
            playerProfile.collect { profile ->
                if (profile != null) {
                    selectedTableFeltName = profile.selectedTableFelt
                }
            }
        }
    }

    // Helper delegate extension to define Compose mutable state in ViewModel
    private var _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    fun showMessage(msg: String) {
        _uiMessage.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_uiMessage.value == msg) _uiMessage.value = null
        }
    }

    // Initialize/Start a fresh 8-ball match
    fun startNewMatch(mode: GameMode, oppName: String, oppDiff: String, levelNum: Int = 1) {
        activeGameMode = mode
        opponentName = oppName
        opponentDifficulty = oppDiff
        activeLevelNumber = levelNum

        physicsEngine.resetTable()

        matchState = MatchState.INTRO_CAMERA
        isPlayerOneTurn = true
        playerOneCategory = null
        playerTwoCategory = null
        shotCountInMatch = 0
        isScratchCommitted = false
        activeFoulMessage = null
        ballsPocketedThisTurn.clear()
        firstBallTouchedThisTurn = null
        currentAimAngle = -PI.toFloat() / 2f
        cueShotPower = 0f

        commentatorLog = if (mode == GameMode.CAREER) {
            "Career Level $levelNum match started against $oppName ($oppDiff). Pocket your target balls!"
        } else if (mode == GameMode.TOURNAMENT) {
            "Tournament ${tournaments[currentTournamentIndex].stageName} matches have commenced! Good luck."
        } else {
            "Online Matchmaking synchronized. Opponent $oppName is ready. Break the rack!"
        }

        // Camera sweep intro delay, then go to aiming
        viewModelScope.launch {
            delay(1800)
            matchState = MatchState.AIMING
        }
    }

    // Simulate online player matchmaking search
    fun triggerOnlineMatchmaking(onSuccess: () -> Unit) {
        matchmakingActive = true
        matchmakingProgress = 0f
        val opponentNames = listOf("Alex_Champs", "Sarah_Vip", "Max_Power_Pool", "Gold_Cue_Master", "Dmitri_Stryker", "Elena_Billiards")
        val difficulties = listOf("Professional", "Master", "Legend")

        viewModelScope.launch {
            while (matchmakingProgress < 1.0f) {
                delay(300)
                matchmakingProgress += 0.15f + Random.nextFloat() * 0.1f
            }
            matchmakingActive = false
            val randomOpp = opponentNames.random()
            val randomDiff = difficulties.random()
            startNewMatch(GameMode.ONLINE_1V1, randomOpp, randomDiff)
            onSuccess()
        }
    }

    // Start a shot backswing pull power
    fun triggerCueShot() {
        if (matchState != MatchState.AIMING) return
        matchState = MatchState.CUE_BACKSWING
    }

    // Launch the physics calculation after striking
    fun executeShot(power: Float) {
        if (matchState != MatchState.CUE_BACKSWING && matchState != MatchState.AIMING) return

        cueShotPower = power
        matchState = MatchState.ROLLING

        // Reset turn parameters
        ballsPocketedThisTurn.clear()
        firstBallTouchedThisTurn = null
        isScratchCommitted = false
        activeFoulMessage = null
        shotCountInMatch++

        commentatorLog = if (isPlayerOneTurn) {
            "You strike the cue ball with ${floor(power * 100f)}% power!"
        } else {
            "$opponentName takes the shot!"
        }

        // Camera shake and cinematic slow-mo for heavy power shots
        if (power > 0.75f) {
            isCameraShaking = true
            isSlowMoActive = true
            viewModelScope.launch {
                delay(400)
                isCameraShaking = false
                delay(800)
                isSlowMoActive = false
            }
        } else if (power > 0.4f) {
            isCameraShaking = true
            viewModelScope.launch {
                delay(200)
                isCameraShaking = false
            }
        }

        // Trigger physics cue ball impulse
        physicsEngine.shootCueBall(currentAimAngle, power)

        // Run the physics ticking loop on the main scope (coroutine-based)
        viewModelScope.launch {
            var collisionOccurredThisShot = false
            withContext(Dispatchers.Default) {
                val dt = 0.016f
                // Run physics steps until everything comes to a complete halt
                while (!physicsEngine.isStationary() && matchState == MatchState.ROLLING) {
                    val result = physicsEngine.tick(dt)
                    val pocketed = result.first
                    val coll = result.second

                    if (coll) collisionOccurredThisShot = true

                    if (pocketed.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            pocketed.forEach { id ->
                                val ball = physicsEngine.balls.find { it.id == id }
                                if (ball != null) {
                                    handleBallPocketedEvent(ball)
                                }
                            }
                        }
                    }
                    delay(12) // roughly 60-80 FPS physical steps simulation
                }
            }

            // After all balls have stopped rolling, evaluate rules
            evaluateTurnResults(collisionOccurredThisShot)
        }
    }

    // Handles rule events when a ball goes in a pocket
    private fun handleBallPocketedEvent(ball: BilliardBall) {
        ballsPocketedThisTurn.add(ball)
        commentatorLog = "Sunk the ${if (ball.type == BallType.CUE) "Cue Ball!" else "${ball.number} Ball (${ball.type})!"}"

        if (ball.type == BallType.CUE) {
            isScratchCommitted = true
            activeFoulMessage = "Cue Ball Scratch! Opponent gains ball-in-hand."
        }
    }

    // Rules verification engine
    private suspend fun evaluateTurnResults(collisionOccurred: Boolean) {
        val cueBall = physicsEngine.balls.find { it.type == BallType.CUE }!!

        // Validate scratches / foul states
        var isFoul = false
        if (cueBall.isPocketed) {
            isFoul = true
            isScratchCommitted = true
            activeFoulMessage = "Cue Ball Scratch! Sunk white ball."
            // Respawn cue ball at initial d-zone
            cueBall.pos = Vector2D(200f, physicsEngine.tableHeight / 2f)
            cueBall.vel = Vector2D(0f, 0f)
            cueBall.isPocketed = false
        }

        if (!isFoul && !collisionOccurred && physicsEngine.balls.count { !it.isPocketed } > 1) {
            isFoul = true
            activeFoulMessage = "Foul: No ball touched on shot."
            commentatorLog = "Foul: Clean miss by the shooter."
        }

        // Did we pocket the 8-ball?
        val eightBallPocketed = ballsPocketedThisTurn.any { it.type == BallType.EIGHT_BALL }
        val activeCategory = if (isPlayerOneTurn) playerOneCategory else playerTwoCategory

        if (eightBallPocketed) {
            // Check if player has cleared all of their target balls
            val targetsRemaining = getRemainingTargetBallsCount(isPlayerOneTurn)
            if (targetsRemaining > 0) {
                // Sunk 8-ball too early! Automatic loss.
                commentatorLog = "Instant Loss! Sunk the 8-ball before clearing category."
                concludeMatch(playerWon = !isPlayerOneTurn)
                return
            } else {
                if (isFoul) {
                    // Sunk 8 ball with foul! Automatic loss.
                    commentatorLog = "Foul Scratch on the 8-ball! Game lost."
                    concludeMatch(playerWon = !isPlayerOneTurn)
                } else {
                    // Clean legal 8-ball sink! VICTORY.
                    commentatorLog = "SENSATIONAL! Sunk the 8-ball for the Match Victory!"
                    concludeMatch(playerWon = isPlayerOneTurn)
                }
                return
            }
        }

        // Assign categories if not yet decided and legal balls pocketed
        val pocketedObjects = ballsPocketedThisTurn.filter { it.type == BallType.SOLID || it.type == BallType.STRIPE }
        if (playerOneCategory == null && pocketedObjects.isNotEmpty() && !isFoul) {
            val firstPocketed = pocketedObjects.first()
            if (isPlayerOneTurn) {
                playerOneCategory = firstPocketed.type
                playerTwoCategory = if (firstPocketed.type == BallType.SOLID) BallType.STRIPE else BallType.SOLID
                commentatorLog = "Category Assigned: You are ${firstPocketed.type}s!"
            } else {
                playerTwoCategory = firstPocketed.type
                playerOneCategory = if (firstPocketed.type == BallType.SOLID) BallType.STRIPE else BallType.SOLID
                commentatorLog = "Category Assigned: Opponent is ${firstPocketed.type}s!"
            }
        }

        // Check if player pocketed one of their own category balls
        val myCategory = if (isPlayerOneTurn) playerOneCategory else playerTwoCategory
        val pocketedMyCategory = ballsPocketedThisTurn.any { it.type == myCategory }

        // Determine who gets the next turn
        if (!isFoul && pocketedMyCategory && ballsPocketedThisTurn.isNotEmpty()) {
            // Keep turn!
            commentatorLog = if (isPlayerOneTurn) {
                "Nice shot! Sunk your target ball. Take another shot."
            } else {
                "$opponentName sinks a target ball and continues."
            }
            matchState = MatchState.AIMING
        } else {
            // Switch turn
            isPlayerOneTurn = !isPlayerOneTurn
            commentatorLog = if (isPlayerOneTurn) {
                "Your turn. Target balls remaining: ${getRemainingTargetBallsCount(true)}."
            } else {
                "$opponentName's turn. Opponent is calculating angles..."
            }

            if (isFoul) {
                matchState = MatchState.FOUL_CUE_PLACEMENT
            } else {
                matchState = MatchState.AIMING
            }
        }

        // Trigger AI or simulated Online turn if it's now opponent's turn!
        if (!isPlayerOneTurn && matchState != MatchState.MATCH_OVER) {
            triggerOpponentTurnLogic()
        }
    }

    private fun getRemainingTargetBallsCount(forPlayerOne: Boolean): Int {
        val category = if (forPlayerOne) playerOneCategory else playerTwoCategory
        if (category == null) return 7 // solid/stripe not assigned yet
        return physicsEngine.balls.count { !it.isPocketed && it.type == category }
    }

    // Intelligent AI/Opponent Cue Solver
    private fun triggerOpponentTurnLogic() {
        isOpponentThinking = true
        viewModelScope.launch {
            // Emulate visual aiming/planning delay
            delay(1800)

            val category = playerTwoCategory
            val cueBall = physicsEngine.balls.find { it.type == BallType.CUE }!!

            // Find valid target balls remaining
            val targets = physicsEngine.balls.filter {
                !it.isPocketed && (if (category != null) it.type == category else (it.type == BallType.SOLID || it.type == BallType.STRIPE))
            }

            var selectedTarget = targets.randomOrNull()
            if (targets.isEmpty()) {
                // Must hit the 8-ball
                selectedTarget = physicsEngine.balls.find { !it.isPocketed && it.type == BallType.EIGHT_BALL }
            }

            if (selectedTarget != null) {
                // Calculate angle from cueball to target
                val delta = selectedTarget.pos - cueBall.pos
                var angle = atan2(delta.y, delta.x)

                // Adjust aim based on AI difficulty tier (adding margin of error)
                val errorFactor = when (opponentDifficulty) {
                    "Beginner" -> 0.16f
                    "Professional" -> 0.08f
                    "Master" -> 0.03f
                    else -> 0.005f // Legend: near perfect
                }

                val offset = (Random.nextFloat() * 2f - 1f) * errorFactor
                angle += offset
                currentAimAngle = angle

                // Pull back backswing simulation
                matchState = MatchState.CUE_BACKSWING
                delay(800)

                // Select matching power based on distance
                val distance = delta.length()
                var shotPower = (distance / 800f + 0.3f).coerceIn(0.2f, 0.95f)
                if (opponentDifficulty == "Beginner") shotPower += (Random.nextFloat() * 0.3f - 0.15f)

                isOpponentThinking = false
                executeShot(shotPower.coerceIn(0.15f, 1.0f))
            } else {
                // Emergency fall back
                currentAimAngle = (Random.nextFloat() * PI * 2).toFloat()
                isOpponentThinking = false
                executeShot(0.5f)
            }
        }
    }

    // Complete the match, distribute scores, achievements, coins, and XP
    private fun concludeMatch(playerWon: Boolean) {
        matchState = MatchState.MATCH_OVER

        val coinsRisked = when (activeGameMode) {
            GameMode.CAREER -> 100
            GameMode.ONLINE_1V1 -> 250
            GameMode.TOURNAMENT -> tournaments[currentTournamentIndex].entryFee
            else -> 0
        }

        viewModelScope.launch {
            if (playerWon) {
                val coinsAwarded = if (activeGameMode == GameMode.TOURNAMENT) {
                    tournaments[currentTournamentIndex].prizeCoins
                } else {
                    coinsRisked * 2
                }

                repository.registerMatchResult(
                    mode = activeGameMode.name,
                    opponentName = opponentName,
                    opponentRating = 1350,
                    playerWon = true,
                    coinsRisked = coinsAwarded
                )
                commentatorLog = "VICTORY! You defeated $opponentName and claimed your rewards!"
                showMessage("Victory! Earned coins & XP.")

                if (activeGameMode == GameMode.CAREER) {
                    repository.completeLevel(activeLevelNumber, stars = 3, score = 1500 + Random.nextInt(500))
                }
            } else {
                repository.registerMatchResult(
                    mode = activeGameMode.name,
                    opponentName = opponentName,
                    opponentRating = 1350,
                    playerWon = false,
                    coinsRisked = coinsRisked
                )
                commentatorLog = "DEFEAT! $opponentName won this match. Better luck next time!"
                showMessage("Match Lost! Keep practicing.")
            }
        }
    }

    // Interactive custom coin spin wheel mechanics
    fun rollLuckySpinWheel() {
        if (isLuckySpinRolling) return
        isLuckySpinRolling = true

        val prizes = listOf("100 Coins", "500 Coins", "2 Premium Cash", "Vip 1-Day Trial", "Free Cue Polish", "1000 Coins")

        viewModelScope.launch {
            delay(2000) // Spin rolling duration
            val prize = prizes.random()
            luckySpinResult = prize
            isLuckySpinRolling = false

            // Award the actual prize to local repository
            when (prize) {
                "100 Coins" -> repository.addRewards(100, 0, 50)
                "500 Coins" -> repository.addRewards(500, 0, 100)
                "1000 Coins" -> repository.addRewards(1000, 0, 200)
                "2 Premium Cash" -> repository.addRewards(0, 2, 50)
                "Vip 1-Day Trial" -> repository.addRewards(200, 5, 100)
                else -> repository.addRewards(100, 0, 50)
            }
        }
    }

    // simulated VIP purchase
    fun simulateVipPurchase() {
        viewModelScope.launch {
            repository.purchaseVip()
            showMessage("VIP Membership Activated successfully!")
        }
    }

    // simulated store item purchases
    fun buyCoinsPackage(amount: Int, costDollars: Double) {
        viewModelScope.launch {
            repository.addRewards(amount, 0, 100)
            showMessage("Successfully purchased $amount Coins!")
        }
    }

    fun buyPremiumPackage(amount: Int, costDollars: Double) {
        viewModelScope.launch {
            repository.addRewards(0, amount, 150)
            showMessage("Successfully purchased $amount Premium Cash!")
        }
    }

    fun upgradeEquippedCue(cueId: String) {
        viewModelScope.launch {
            repository.upgradeCue(cueId)
            showMessage("Cue upgraded! Stats boosted.")
        }
    }

    fun equipCue(cueId: String) {
        viewModelScope.launch {
            repository.equipCue(cueId)
            showMessage("Cue equipped!")
        }
    }

    fun buyCue(cueId: String, currencyType: String) {
        viewModelScope.launch {
            repository.buyCue(cueId, currencyType)
            showMessage("Cue unlocked successfully!")
        }
    }

    fun updateTableFelt(feltName: String) {
        viewModelScope.launch {
            repository.updateTableFelt(feltName)
            showMessage("Table Felt customized to $feltName!")
        }
    }
}
