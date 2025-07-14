package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.domain.entity.QuestionDifficulty
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    suspend fun getRandomQuestion(category: QuestionCategory? = null, difficulty: QuestionDifficulty? = null): Question?
    suspend fun getQuestionsByCategory(category: QuestionCategory): Flow<List<Question>>
    suspend fun getQuestionsByDifficulty(difficulty: QuestionDifficulty): Flow<List<Question>>
    suspend fun getFavoriteQuestions(): Flow<List<Question>>
    suspend fun toggleFavorite(questionId: String)
    suspend fun addQuestion(question: Question): String
    suspend fun updateQuestion(question: Question)
    suspend fun deleteQuestion(questionId: String)
} 