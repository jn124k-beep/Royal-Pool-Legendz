package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Pool Legend",
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 1000,
    val premiumCurrency: Int = 50,
    val isVip: Boolean = false,
    val equippedCueId: String = "classic_wood",
    val wins: Int = 0,
    val losses: Int = 0,
    val rankPoints: Int = 1200,
    val streak: Int = 0,
    val lastDailyRewardTime: Long = 0L,
    val selectedTableFelt: String = "Emerald Green"
)

@Entity(tableName = "cue_items")
data class CueItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Wood", "Pro", "Carbon", "Gold", "Diamond", "Legendary"
    val power: Float,     // 0.0 to 1.0
    val accuracy: Float,  // 0.0 to 1.0
    val spin: Float,      // 0.0 to 1.0
    val timeExtension: Float, // 0.0 to 1.0
    val costCoins: Int,
    val costPremium: Int,
    val isOwned: Boolean,
    val isEquipped: Boolean,
    val upgradeLevel: Int = 1
)

@Entity(tableName = "career_level_progress")
data class CareerLevelProgress(
    @PrimaryKey val levelNumber: Int,
    val stars: Int = 0,
    val maxScore: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "match_history")
data class MatchHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String, // "Career", "Online 1v1", "Tournament"
    val opponentName: String,
    val opponentRating: Int,
    val playerWon: Boolean,
    val coinsEarned: Int,
    val date: Long = System.currentTimeMillis()
)
