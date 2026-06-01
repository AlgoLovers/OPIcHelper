package com.na982.opichelper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qa_items")
data class QaItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val itemId: String,
    val level: String,
    val questionEn: String,
    val questionKo: String,
    val answerEn: String,
    val answerKo: String,
    val vocabulary: String = "",
    val grammar: String = "",
    val tips: String = "",
    val questionEnOriginal: String,
    val questionKoOriginal: String,
    val answerEnOriginal: String,
    val answerKoOriginal: String,
    val isModified: Boolean = false,
    val updatedAt: Long = 0L
)
