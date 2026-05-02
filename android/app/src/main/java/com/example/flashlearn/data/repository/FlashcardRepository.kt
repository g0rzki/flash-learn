package com.example.flashlearn.data.repository

import android.util.Log
import com.example.flashlearn.data.remote.FlashcardApiService
import com.example.flashlearn.sync.SyncManager
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val deckDao: DeckDao,
    private val syncManager: SyncManager,
    private val flashcardApi: FlashcardApiService
) {
    fun observeByDeck(deckId: Long): Flow<List<Flashcard>> =
        flashcardDao.observeByDeck(deckId)

    suspend fun getById(id: Long): Flashcard? = flashcardDao.getById(id)

    suspend fun saveFlashcard(
        flashcardId: Long?,
        deckId: Long,
        question: String,
        answer: String
    ) {
        val now = Instant.now().epochSecond
        if (flashcardId != null) {
            val existing = flashcardDao.getById(flashcardId)
            if (existing != null) {
                flashcardDao.update(
                    existing.copy(
                        question = question,
                        answer = answer,
                        updatedAt = now,
                        needsSync = true
                    )
                )
            }
        } else {
            flashcardDao.insert(
                Flashcard(
                    deckId = deckId,
                    question = question,
                    answer = answer,
                    needsSync = true
                )
            )
        }
        syncManager.scheduleSync()
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) {
        if (flashcard.serverId != null) {
            val deck = deckDao.getById(flashcard.deckId)
            if (deck?.serverId != null) {
                try {
                    flashcardApi.deleteFlashcard(deck.serverId, flashcard.serverId)
                } catch (e: Exception) {
                    Log.e("FlashcardRepository", "Failed to delete flashcard from server", e)
                }
            }
        }
        flashcardDao.delete(flashcard)
        syncManager.scheduleSync()
    }
}
