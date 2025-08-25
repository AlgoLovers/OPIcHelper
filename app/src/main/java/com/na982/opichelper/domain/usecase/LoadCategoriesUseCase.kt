package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.QaDataRepository
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 카테고리 목록을 로드하는 UseCase
 */
@ViewModelScoped
class LoadCategoriesUseCase @Inject constructor(
    private val qaDataRepository: QaDataRepository
) {
    suspend operator fun invoke(): List<String> {
        return qaDataRepository.categories.value
    }
} 