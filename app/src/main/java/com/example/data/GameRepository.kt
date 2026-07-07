package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val gameDao: GameDao) {

    val playerProfile: Flow<PlayerProfile?> = gameDao.getPlayerProfile()
    val allCues: Flow<List<CueItem>> = gameDao.getAllCues()
    val careerProgress: Flow<List<CareerLevelProgress>> = gameDao.getCareerProgress()
    val recentMatches: Flow<List<MatchHistoryItem>> = gameDao.getRecentMatchHistory()
    val completedLevelsCount: Flow<Int> = gameDao.getCompletedLevelsCount()

    suspend fun initializeDefaultsIfNecessary() = withContext(Dispatchers.IO) {
        val existingProfile = playerProfile.firstOrNull()
        if (existingProfile == null) {
            gameDao.insertOrUpdateProfile(PlayerProfile())
        }

        val existingCues = allCues.firstOrNull()
        if (existingCues.isNullOrEmpty()) {
            val defaultCues = listOf(
                CueItem("classic_wood", "Classic Wood", "Classic", 0.35f, 0.40f, 0.25f, 0.20f, 0, 0, true, true),
                CueItem("pro_carbon", "Carbon Viper", "Professional", 0.55f, 0.50f, 0.50f, 0.40f, 1500, 0, false, false),
                CueItem("aurum_gold", "Royal Aurum", "Gold Edition", 0.75f, 0.70f, 0.65f, 0.60f, 5000, 20, false, false),
                CueItem("cyber_plasma", "Plasma Neon", "Diamond Edition", 0.88f, 0.85f, 0.80f, 0.75f, 15000, 50, false, false),
                CueItem("pool_conqueror", "Cosmic Conqueror", "Legendary", 0.98f, 0.95f, 0.95f, 0.90f, 50000, 150, false, false)
            )
            gameDao.insertCues(defaultCues)
        }

        val existingLevels = careerProgress.firstOrNull()
        if (existingLevels.isNullOrEmpty()) {
            // Pre-populate some levels as active/unlocked.
            // Since we can have 5000+ levels, we'll lazily handle level details,
            // but we can pre-populate the first 10 levels for direct tracking!
            for (i in 1..15) {
                gameDao.insertLevelProgress(CareerLevelProgress(levelNumber = i, stars = 0, maxScore = 0, isCompleted = false))
            }
        }
    }

    suspend fun addRewards(coins: Int, premium: Int, xp: Int) = withContext(Dispatchers.IO) {
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        val newXp = profile.xp + xp
        // Level up formula: each level takes Level * 1000 XP
        val newLevel = calculateLevelForXp(newXp)
        val newProfile = profile.copy(
            coins = profile.coins + coins,
            premiumCurrency = profile.premiumCurrency + premium,
            xp = newXp,
            level = newLevel
        )
        gameDao.insertOrUpdateProfile(newProfile)
    }

    private fun calculateLevelForXp(xp: Int): Int {
        var remainingXp = xp
        var level = 1
        while (remainingXp >= level * 1000) {
            remainingXp -= level * 1000
            level++
        }
        return level
    }

    suspend fun equipCue(cueId: String) = withContext(Dispatchers.IO) {
        val cues = allCues.firstOrNull() ?: return@withContext
        val updatedCues = cues.map { cue ->
            cue.copy(isEquipped = cue.id == cueId)
        }
        gameDao.insertCues(updatedCues)

        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        gameDao.insertOrUpdateProfile(profile.copy(equippedCueId = cueId))
    }

    suspend fun buyCue(cueId: String, currencyType: String) = withContext(Dispatchers.IO) {
        val cue = gameDao.getCueById(cueId) ?: return@withContext
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()

        if (currencyType == "coins" && profile.coins >= cue.costCoins) {
            val updatedCue = cue.copy(isOwned = true)
            gameDao.updateCue(updatedCue)
            gameDao.insertOrUpdateProfile(profile.copy(coins = profile.coins - cue.costCoins))
            equipCue(cueId)
        } else if (currencyType == "premium" && profile.premiumCurrency >= cue.costPremium) {
            val updatedCue = cue.copy(isOwned = true)
            gameDao.updateCue(updatedCue)
            gameDao.insertOrUpdateProfile(profile.copy(premiumCurrency = profile.premiumCurrency - cue.costPremium))
            equipCue(cueId)
        }
    }

    suspend fun upgradeCue(cueId: String) = withContext(Dispatchers.IO) {
        val cue = gameDao.getCueById(cueId) ?: return@withContext
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        val upgradeCost = cue.upgradeLevel * 800

        if (profile.coins >= upgradeCost && cue.isOwned) {
            val updatedCue = cue.copy(
                upgradeLevel = cue.upgradeLevel + 1,
                power = (cue.power + 0.03f).coerceAtMost(1.0f),
                accuracy = (cue.accuracy + 0.02f).coerceAtMost(1.0f),
                spin = (cue.spin + 0.03f).coerceAtMost(1.0f)
            )
            gameDao.updateCue(updatedCue)
            gameDao.insertOrUpdateProfile(profile.copy(coins = profile.coins - upgradeCost))
        }
    }

    suspend fun completeLevel(levelNumber: Int, stars: Int, score: Int) = withContext(Dispatchers.IO) {
        gameDao.insertLevelProgress(CareerLevelProgress(levelNumber, stars, score, true))
        // Auto-unlock next level
        val nextLevel = levelNumber + 1
        gameDao.insertLevelProgress(CareerLevelProgress(nextLevel, 0, 0, false))

        // Award rewards
        addRewards(coins = 150 + (stars * 50), premium = if (levelNumber % 5 == 0) 2 else 0, xp = 200 + (stars * 50))
    }

    suspend fun registerMatchResult(mode: String, opponentName: String, opponentRating: Int, playerWon: Boolean, coinsRisked: Int) = withContext(Dispatchers.IO) {
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        val coinsWon = if (playerWon) coinsRisked else -coinsRisked
        val xpGained = if (playerWon) 300 else 100
        val rankPointsDelta = if (playerWon) 25 else -15

        val newWins = if (playerWon) profile.wins + 1 else profile.wins
        val newLosses = if (!playerWon) profile.losses + 1 else profile.losses
        val newStreak = if (playerWon) profile.streak + 1 else 0
        val newRank = (profile.rankPoints + rankPointsDelta).coerceAtLeast(100)

        val newProfile = profile.copy(
            coins = (profile.coins + coinsWon).coerceAtLeast(0),
            wins = newWins,
            losses = newLosses,
            streak = newStreak,
            rankPoints = newRank
        )
        gameDao.insertOrUpdateProfile(newProfile)
        addRewards(coins = 0, premium = 0, xp = xpGained)

        gameDao.insertMatchHistory(
            MatchHistoryItem(
                mode = mode,
                opponentName = opponentName,
                opponentRating = opponentRating,
                playerWon = playerWon,
                coinsEarned = coinsWon
            )
        )
    }

    suspend fun purchaseVip() = withContext(Dispatchers.IO) {
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        gameDao.insertOrUpdateProfile(
            profile.copy(
                isVip = true,
                coins = profile.coins + 10000,
                premiumCurrency = profile.premiumCurrency + 150
            )
        )
    }

    suspend fun updateTableFelt(feltName: String) = withContext(Dispatchers.IO) {
        val profile = playerProfile.firstOrNull() ?: PlayerProfile()
        gameDao.insertOrUpdateProfile(profile.copy(selectedTableFelt = feltName))
    }
}
