package com.na982.opichelper.data.audio

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.na982.opichelper.domain.audio.HighlightManager
import com.na982.opichelper.domain.audio.HighlightStrategy
import com.na982.opichelper.domain.audio.HighlightType
import com.na982.opichelper.domain.event.HighlightEvent
import com.na982.opichelper.domain.event.HighlightEventHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하이라이트 매니저 구현체
 * 모든 재생 관련 하이라이트를 중앙에서 관리합니다.
 */
@Singleton
class HighlightManagerImpl @Inject constructor() : HighlightManager {
    
    private var currentStrategy: HighlightStrategy? = null
    private var highlightHandler: Handler? = null
    private var highlightRunnable: Runnable? = null
    private var currentHighlightIndex: Int = -1
    private var currentHighlightType: HighlightType? = null
    private var eventHandler: HighlightEventHandler? = null
    private var currentPosition: Int = -1
    
    override fun setStrategy(strategy: HighlightStrategy) {
        Log.d("HighlightManagerImpl", "전략 설정: ${strategy.getHighlightType()}")
        currentStrategy = strategy
        currentHighlightType = strategy.getHighlightType()
    }
    
    override fun startHighlightUpdates(eventHandler: HighlightEventHandler) {
        Log.d("HighlightManagerImpl", "하이라이트 업데이트 시작")
        
        // 기존 업데이트 중지
        stopHighlightUpdates()
        
        currentStrategy?.let { strategy ->
            if (!strategy.isValid()) {
                Log.w("HighlightManagerImpl", "전략이 유효하지 않음: ${strategy.getHighlightType()}")
                return
            }
            
            this.eventHandler = eventHandler
            highlightHandler = Handler(Looper.getMainLooper())
            
            highlightRunnable = object : Runnable {
                override fun run() {
                    if (currentPosition >= 0) {
                        val highlightIndex = strategy.calculateHighlightIndex(currentPosition)
                        if (highlightIndex != currentHighlightIndex) {
                            currentHighlightIndex = highlightIndex
                            
                            // 이벤트 발생
                            val event = HighlightEvent.UpdateHighlight(
                                type = strategy.getHighlightType(),
                                index = highlightIndex
                            )
                            eventHandler.handle(event)
                            
                            Log.d("HighlightManagerImpl", "하이라이트 이벤트 발생: 타입=${strategy.getHighlightType()}, 인덱스=$highlightIndex")
                        }
                    }
                    
                    // 100ms마다 업데이트
                    highlightHandler?.postDelayed(this, 100)
                }
            }
            
            highlightHandler?.post(highlightRunnable!!)
        } ?: run {
            Log.w("HighlightManagerImpl", "전략이 설정되지 않음")
        }
    }
    
    override fun stopHighlightUpdates() {
        Log.d("HighlightManagerImpl", "하이라이트 업데이트 중지")
        highlightRunnable?.let { runnable ->
            highlightHandler?.removeCallbacks(runnable)
        }
        highlightRunnable = null
        highlightHandler = null
        currentHighlightIndex = -1
        eventHandler = null
    }
    
    override fun getCurrentHighlightIndex(): Int = currentHighlightIndex
    
    override fun getCurrentHighlightType(): HighlightType? = currentHighlightType
    
    override fun stopAllHighlights() {
        Log.d("HighlightManagerImpl", "모든 하이라이트 중지")
        stopHighlightUpdates()
        currentStrategy = null
        currentHighlightType = null
        currentHighlightIndex = -1
        
        // 모든 하이라이트 제거 이벤트 발생
        eventHandler?.handle(HighlightEvent.ClearAllHighlights)
    }
    
    override fun updateCurrentPosition(currentPositionMs: Int) {
        currentPosition = currentPositionMs
    }
} 