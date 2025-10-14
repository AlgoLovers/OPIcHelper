package com.na982.opichelper.di

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.data.audio.AudioPlayerImpl
import com.na982.opichelper.data.audio.AudioRecorderImpl
import com.na982.opichelper.data.audio.GoogleTtsPlayer
import com.na982.opichelper.data.audio.HighlightManagerImpl
import com.na982.opichelper.data.audio.RecordingAudioPlayerImpl
import com.na982.opichelper.data.audio.SamsungTtsPlayer
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import com.na982.opichelper.data.repository.AuthRepositoryImpl
import com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl
import com.na982.opichelper.data.repository.FullMemorizationRepositoryImpl
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.QaDataLoaderImpl
import com.na982.opichelper.data.repository.RecordingFileRepositoryImpl
import com.na982.opichelper.data.repository.RecordingTimeManagerImpl
import com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl
import com.na982.opichelper.data.repository.UserPreferencesRepositoryImpl
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.manager.WakeLockManager
import com.na982.opichelper.domain.manager.AudioControlManager
import com.na982.opichelper.domain.manager.MemorizationManager
import com.na982.opichelper.domain.manager.ICategoryManager
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.IMemorizationManager
import com.na982.opichelper.domain.manager.ProgressManager
import com.na982.opichelper.domain.manager.TtsHealthMonitor
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.data.state.AppStateManagerImpl
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import com.na982.opichelper.domain.usecase.InitializeAppUseCase
import com.na982.opichelper.domain.strategy.MemorizationStrategyFactory
import com.na982.opichelper.domain.strategy.RepeatListeningStrategy
import com.na982.opichelper.domain.strategy.EnglishWritingStrategy
import com.na982.opichelper.domain.strategy.FullMemorizationStrategy
import com.na982.opichelper.domain.strategy.MemorizationLevelMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import com.na982.opichelper.data.repository.LearningPreferencesRepositoryImpl
import com.na982.opichelper.data.repository.TtsSettingsRepositoryImpl
import com.na982.opichelper.domain.repository.LearningPreferencesRepository
import com.na982.opichelper.domain.repository.TtsSettingsRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ===== Data Layer Implementations =====
    
    // AudioRecorder와 RecordingAudioPlayer는 ActivityModule로 이동됨
    
    @Provides
    @Singleton
    fun provideAudioPlayer(): AudioPlayer {
        return AudioPlayerImpl()
    }
    
    @Provides
    @Singleton
    fun provideHighlightManager(): com.na982.opichelper.domain.audio.HighlightManager {
        return HighlightManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideHighlightEventHandler(appStateManager: AppStateManager): com.na982.opichelper.domain.event.HighlightEventHandler {
        return com.na982.opichelper.domain.event.HighlightEventHandler(appStateManager)
    }

    @Provides
    @Singleton
    fun provideAudioFileManager(@ApplicationContext context: Context): AudioFileManager {
        return AudioFileManagerImpl(context)
    }
    
    // ===== TTS Players =====
    
    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return GoogleTtsPlayer(context)
    }
    
    @Provides
    @Singleton
    @Named("samsung")
    fun provideSamsungTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return SamsungTtsPlayer(context)
    }
    
    @Provides
    @Singleton
    fun provideTtsOrchestrator(
        @ApplicationContext context: Context,
        @Named("google") googleTtsPlayer: TtsPlayer,
        @Named("samsung") samsungTtsPlayer: TtsPlayer,
        ttsHealthMonitor: TtsHealthMonitor,
        appStateManager: AppStateManager
    ): TtsOrchestrator {
        return TtsOrchestrator(context, googleTtsPlayer, samsungTtsPlayer, ttsHealthMonitor, appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideTtsHealthMonitor(
        appStateManager: AppStateManager
    ): TtsHealthMonitor {
        return TtsHealthMonitor(appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideTtsController(
        ttsOrchestrator: TtsOrchestrator,
        appStateManager: AppStateManager
    ): TtsController {
        return com.na982.opichelper.data.audio.TtsControllerImpl(
            ttsOrchestrator = ttsOrchestrator,
            appStateManager = appStateManager
        )
    }
    
    // ===== Repository Layer =====
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataLoader(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository
    ): QaDataLoader {
        return QaDataLoaderImpl(context, userPreferencesRepository)
    }
    
    @Provides
    @Singleton
    fun provideLeveledQaDataLoader(
        @ApplicationContext context: Context
    ): LeveledQaDataLoader {
        return LeveledQaDataLoader(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataRepository(
        leveledQaDataLoader: LeveledQaDataLoader,
        userPreferencesRepository: UserPreferencesRepository
    ): QaDataRepository {
        return QaDataRepository(leveledQaDataLoader, userPreferencesRepository)
    }
    

    

    
    // Repository들은 ActivityComponent나 ViewModelComponent 의존성 때문에 AppModule에서 제공 불가
    
    // ===== Manager Classes =====
    
    @Provides
    @Singleton
    fun provideWakeLockManager(@ApplicationContext context: Context): WakeLockManager {
        return WakeLockManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAppStateManager(): AppStateManager {
        return AppStateManagerImpl()
    }
    
    // Manager Classes - ViewModelModule로 이동됨
    
    @Provides
    @Singleton
    fun provideRecordingTimeManager(@ApplicationContext context: Context): RecordingTimeManager {
        return RecordingTimeManagerImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideProgressPersistenceService(@ApplicationContext context: Context): ProgressPersistenceService {
        return ProgressPersistenceService(context)
    }
    
    // ===== UseCase Layer =====
    // ViewModelModule로 이동됨
    
    // ===== Strategy Pattern =====
    // ViewModelModule로 이동됨
    

    

    
    // ===== Event Handlers =====
    // ViewModelModule로 이동됨
} 