package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

interface QaDataManager :
    QaContentReader,
    QaNavigator,
    QaSearch,
    QaDataLifecycle {

    val currentQaItem: StateFlow<QaItem?>
    val currentCategory: StateFlow<String?>
    val categories: StateFlow<List<String>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
}
