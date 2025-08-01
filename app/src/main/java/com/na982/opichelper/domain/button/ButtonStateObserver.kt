package com.na982.opichelper.domain.button

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState

/**
 * 버튼 상태 관찰자 인터페이스
 * 버튼 상태 변경을 감지하고 처리
 */
interface ButtonStateObserver {
    /**
     * 버튼 상태 변경 시 호출
     * @param buttonFunction 변경된 버튼
     * @param newState 새로운 상태
     */
    fun onButtonStateChanged(buttonFunction: ButtonFunction, newState: ButtonState)
    
    /**
     * 모든 버튼이 초기화될 때 호출
     */
    fun onAllButtonsReset()
} 