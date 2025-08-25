package com.na982.opichelper.di

import android.content.Context
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.data.audio.AudioRecorderImpl
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.data.audio.RecordingAudioPlayerImpl
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.AudioControlManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.domain.usecase.PlayRecordingUseCase
import com.na982.opichelper.domain.audio.HighlightManager
import com.na982.opichelper.domain.event.HighlightEventHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped

/**
 * MainActivity 전용 의존성을 관리하는 모듈
 * ActivityComponent 범위에서만 사용됨
 * MainActivity와 생명주기를 같이 하는 객체들을 관리
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    
    @Provides
    @ActivityScoped
    fun provideRecordingAudioPlayer(
        recordingTimeManager: RecordingTimeManager,
        highlightManager: com.na982.opichelper.domain.audio.HighlightManager,
        highlightEventHandler: com.na982.opichelper.domain.event.HighlightEventHandler
    ): RecordingAudioPlayer {
        return RecordingAudioPlayerImpl(recordingTimeManager, highlightManager, highlightEventHandler)
    }
    
    @Provides
    @ActivityScoped
    fun provideAudioControlManager(
        ttsController: TtsController,
        recordingAudioPlayer: RecordingAudioPlayer,
        ttsOrchestrator: TtsOrchestrator
    ): IAudioControlManager {
        return AudioControlManager(
            ttsController = ttsController,
            recordingAudioPlayer = recordingAudioPlayer,
            ttsOrchestrator = ttsOrchestrator
        )
    }
    
    // Activity 범위 제공 제거: ViewModelModule에서 ViewModelScoped로 제공
    
    // Activity 범위 제공 제거: ViewModelModule에서 ViewModelScoped로 제공
    
    // Activity 범위 제공 제거: ViewModelModule에서 ViewModelScoped로 제공
    
    // Activity 범위 제공 제거: ViewModelModule에서 ViewModelScoped로 제공
}
