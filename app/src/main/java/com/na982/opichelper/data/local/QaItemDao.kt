package com.na982.opichelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface QaItemDao {
    @Query("SELECT * FROM qa_items WHERE level = :level ORDER BY category, CAST(itemId AS INTEGER)")
    suspend fun getByCategoryAndLevelDirect(level: String): List<QaItemEntity>

    @Query("SELECT * FROM qa_items WHERE id = :id")
    suspend fun getById(id: String): QaItemEntity?

    @Update
    suspend fun update(item: QaItemEntity)

    @Query("""
        UPDATE qa_items SET
        questionEn = questionEnOriginal, questionKo = questionKoOriginal,
        answerEn = answerEnOriginal, answerKo = answerKoOriginal,
        isModified = 0, updatedAt = 0
        WHERE id = :id
    """)
    suspend fun restoreOriginal(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QaItemEntity>)

    @Query("SELECT COUNT(*) FROM qa_items")
    suspend fun getCount(): Int
}
