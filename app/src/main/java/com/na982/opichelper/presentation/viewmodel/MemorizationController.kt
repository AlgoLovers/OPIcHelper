package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.ModeGroup

class MemorizationController(
    viewModels: Map<ModeGroup, BaseMemorizationViewModel<*>>
) {
    private val viewModels: Map<ModeGroup, BaseMemorizationViewModel<*>> = viewModels.toMap()
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

    fun getViewModelForGroup(group: ModeGroup): BaseMemorizationViewModel<*>? {
        return viewModels[group]
    }
}
