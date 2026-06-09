package com.na982.opichelper.presentation.ui.screen

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel

data class EditScriptState(
    val qaItem: QaItem,
    val isQuestion: Boolean,
    val level: UserLevel,
    val scriptIndex: Int,
    val entityId: String
)
