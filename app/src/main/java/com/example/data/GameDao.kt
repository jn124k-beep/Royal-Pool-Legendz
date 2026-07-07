package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Player Profile
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfile)

    // Cues
    @Query("SELECT * FROM cue_items ORDER BY costCoins ASC, costPremium ASC")
    fun getAllCues(): Flow<List<CueItem>>

    @Query("SELECT * FROM cue_items WHERE id = :cueId")
    suspend fun getCueById(cueId: String): CueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCues(cues: List<CueItem>)

    @Update
    suspend fun updateCue(cue: CueItem)

    // Career Levels Progress
    @Query("SELECT * FROM career_level_progress ORDER BY levelNumber ASC")
    fun getCareerProgress(): Flow<List<CareerLevelProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevelProgress(progress: CareerLevelProgress)

    @Query("SELECT COUNT(*) FROM career_level_progress WHERE isCompleted = 1")
    fun getCompletedLevelsCount(): Flow<Int>

    // Match History
    @Query("SELECT * FROM match_history ORDER BY date DESC LIMIT 20")
    fun getRecentMatchHistory(): Flow<List<MatchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistoryItem)
}
