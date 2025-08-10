package com.na982.opichelper.domain.manager

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * 오디오 제어 인터페이스
 * 단일 책임: 오디오 재생만 담당
 */
interface IAudioControlManager {
    
    /**
     * 에러 상태
     */
    val error: StateFlow<String?>
    
    /**
     * 질문 재생
     */
    fun playQuestion(qaItem: QaItem, onCompletion: () -> Unit)
    
    /**
     * 답변 재생
     */
    fun playAnswer(qaItem: QaItem, onCompletion: () -> Unit)
    
    /**
     * 모든 오디오 중지 (일시 중지 - 재생 중단용)
     * - 재생 중 같은 버튼 클릭
     * - 재생 중 다른 버튼 클릭
     * - 카테고리/암기레벨 변경 시
     */
    fun stopAllAudio()
    
    /**
     * 특정 버튼의 오디오 중지
     */
    fun stopSpecificAudio(buttonFunction: String)
    
    /**
     * 모든 오디오 중지 및 TTS 플레이어 해제 (완전 종료용)
     * - 앱 백키로 종료 시
     * - 앱 완전 종료 시
     */
    fun releaseAllAudio()
    
    /**
     * TTS 플레이어 재초기화 (release 후 재사용 시)
     * - 앱 재시작 시
     * - TTS 오류 복구 시
     */
    fun reinitializeTtsPlayers()
    
    /**
     * 동기적 TTS 정리 (앱 종료 시 사용)
     * - 백키로 앱 종료 시
     * - 앱 완전 종료 시
     */
    suspend fun cleanupTtsSync()
} 