package com.na982.opichelper.data.repository

import com.na982.opichelper.data.local.QaItemDao
import com.na982.opichelper.data.local.toQaItem
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.ScriptEditRepository
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScriptEditRepositoryImpl(
    private val dao: QaItemDao,
    private val recordingTimeManager: RecordingTimeManager,
    private val progressPersistenceService: ProgressPersistenceService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ScriptEditRepository {

    override fun getQaItemsByCategory(category: String, level: String): Flow<List<QaItem>> =
        dao.getByCategoryAndLevel(category, level).map { entities ->
            entities.map { it.toQaItem() }
        }

    override suspend fun updateQaItem(item: QaItem, level: UserLevel, scriptIndex: Int) {
        val id = generateId(item, level)
        val entity = dao.getById(id) ?: return
        val sentenceCountChanged = hasSentenceCountChanged(entity, item, level)

        dao.update(entity.copy(
            questionEn = item.questionEn,
            questionKo = item.questionKo,
            answerEn = item.answers[level]?.answerEn ?: entity.answerEn,
            answerKo = item.answers[level]?.answerKo ?: entity.answerKo,
            isModified = true,
            updatedAt = System.currentTimeMillis()
        ))

        if (sentenceCountChanged) {
            recordingTimeManager.clearRecordingTimes(item.category, scriptIndex)
            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(item.category, scriptIndex, 0)
            )
            for (memLevel in MemorizeLevel.entries) {
                progressPersistenceService.clearCategoryProgress(item.category, scriptIndex, memLevel.displayName)
            }
        }
    }

    override suspend fun restoreOriginal(id: String) = dao.restoreOriginal(id)

    override suspend fun restoreAllOriginal() = dao.restoreAllOriginal()

    override suspend fun isModified(id: String): Boolean = dao.getById(id)?.isModified ?: false

    private fun generateId(item: QaItem, level: UserLevel): String =
        "${item.category}_${item.id}_${level.name}"

    private fun hasSentenceCountChanged(entity: com.na982.opichelper.data.local.QaItemEntity, item: QaItem, level: UserLevel): Boolean {
        val oldCount = SentenceSplitter.split(entity.answerEn).size
        val newCount = SentenceSplitter.split(item.answers[level]?.answerEn ?: "").size
        return oldCount != newCount
    }
}
