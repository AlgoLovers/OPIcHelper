package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.ModeGroup
import com.na982.opichelper.domain.entity.toModeGroup
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator

class MemorizationController(
    viewModels: Map<ModeGroup, BaseMemorizationViewModel<*>>
) {
    private val viewModels: Map<ModeGroup, BaseMemorizationViewModel<*>> = viewModels.toMap()
    fun startForGroup(group: ModeGroup) {
        viewModels[group]?.start()
    }

    fun startForLevel(selectedLevel: String) {
        val level = MemorizeLevel.fromDisplayName(selectedLevel)
        startForGroup(level.toModeGroup())
    }

    fun stopForGroup(group: ModeGroup) {
        viewModels[group]?.stop()
    }

    fun stopCurrent(coordinator: MemorizationModeCoordinator) {
        stopForGroup(coordinator.currentMode.value.group)
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
