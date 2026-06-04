package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.usecase.ModeGroup

class MemorizationController(
    private val viewModels: Map<ModeGroup, BaseMemorizationViewModel<*>>
) {
    fun startForGroup(group: ModeGroup) {
        viewModels[group]?.start()
    }

    fun stopForGroup(group: ModeGroup) {
        viewModels[group]?.stop()
    }

    fun stopAll() {
        viewModels.values.forEach { it.stop() }
    }

    fun onLevelChangedAll() {
        viewModels.values.forEach { it.onLevelChanged() }
    }
}
