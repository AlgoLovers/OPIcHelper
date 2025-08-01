package com.na982.opichelper.di

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.data.audio.AudioPlayerImpl
import com.na982.opichelper.data.audio.AudioRecorderImpl
import com.na982.opichelper.data.audio.GoogleTtsPlayer
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Data Layer Implementations
    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorderImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioPlayer(): AudioPlayer {
        return AudioPlayerImpl()
    }
    
    @Provides
    @Singleton
    fun provideRecordingAudioPlayer(): RecordingAudioPlayer {
        return RecordingAudioPlayerImpl()
    }
    
    @Provides
    @Singleton
    fun provideAudioFileManager(@ApplicationContext context: Context): AudioFileManager {
        return AudioFileManagerImpl(context)
    }
    
    // TTS Players
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
        @Named("samsung") samsungTtsPlayer: TtsPlayer
    ): TtsOrchestrator {
        return TtsOrchestrator(context, googleTtsPlayer, samsungTtsPlayer)
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
    fun provideProgressPersistenceService(@ApplicationContext context: Context): ProgressPersistenceService {
        return ProgressPersistenceService(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataRepository(
        leveledQaDataLoader: LeveledQaDataLoader,
        userPreferencesRepository: UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.QaDataRepository {
        return com.na982.opichelper.domain.repository.QaDataRepository(leveledQaDataLoader, userPreferencesRepository)
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideRecordingTimeManager(@ApplicationContext context: Context): RecordingTimeManager {
        return RecordingTimeManagerImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideRecordingFileRepository(
        audioFileManager: AudioFileManager,
        audioRecorder: AudioRecorder,
        recordingAudioPlayer: RecordingAudioPlayer,
        recordingTimeManager: RecordingTimeManager
    ): RecordingFileRepository {
        return RecordingFileRepositoryImpl(audioFileManager, audioRecorder, recordingAudioPlayer, recordingTimeManager)
    }
    
    // WakeLock Manager
    @Provides
    @Singleton
    fun provideWakeLockManager(@ApplicationContext context: Context): WakeLockManager {
        return WakeLockManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): com.na982.opichelper.domain.repository.AuthRepository {
        return AuthRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideLeveledQaDataLoader(@ApplicationContext context: Context): LeveledQaDataLoader {
        return LeveledQaDataLoader(context)
    }
    
    @Provides
    @Singleton
    fun provideEnglishWritingTestRepository(
        qaDataRepository: QaDataRepository,
        ttsOrchestrator: TtsOrchestrator,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
    ): EnglishWritingTestRepository {
        return EnglishWritingTestRepositoryImpl(
            qaDataRepository = qaDataRepository,
            ttsOrchestrator = ttsOrchestrator,
            audioRecorder = audioRecorder,
            audioFileManager = audioFileManager,
            recordingTimeManager = recordingTimeManager,
            progressTracker = progressTracker
        )
    }
    
    @Provides
    @Singleton
    fun provideRepeatListeningRepository(
        repeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
    ): RepeatListeningRepository {
        return RepeatListeningRepositoryImpl(
            repeatListeningUseCase = repeatListeningUseCase,
            progressTracker = progressTracker
        )
    }
    
    @Provides
    @Singleton
    fun provideFullMemorizationRepository(
        ttsOrchestrator: TtsOrchestrator,
        audioRecorder: AudioRecorder,
        audioPlayer: AudioPlayer,
        audioFileManager: AudioFileManager,
        qaDataRepository: QaDataRepository
    ): FullMemorizationRepository {
        return FullMemorizationRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            audioRecorder = audioRecorder,
            audioPlayer = audioPlayer,
            audioFileManager = audioFileManager,
            qaDataRepository = qaDataRepository
        )
    }
    
    // 새로운 버튼 관리 클래스들
    @Provides
    @Singleton
    fun provideButtonStateObserver(ttsOrchestrator: TtsOrchestrator): com.na982.opichelper.domain.button.ButtonStateObserver {
        return ttsOrchestrator
    }
    
    @Provides
    @Singleton
    fun provideAppStateManager(): AppStateManager {
        return AppStateManagerImpl()
    }
    

    
    @Provides
    @Singleton
    fun provideRepeatListeningUseCase(
        ttsController: TtsController,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker,
        recordingTimeManager: RecordingTimeManager
    ): com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase {
        return com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase(
            ttsController = ttsController,
            progressTracker = progressTracker,
            recordingTimeManager = recordingTimeManager
        )
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
    

    
    // Strategy Pattern 관련 의존성 주입
    @Provides
    @Singleton
    fun provideRepeatListeningStrategy(
        repeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
    ): RepeatListeningStrategy {
        return RepeatListeningStrategy(repeatListeningUseCase)
    }
    
    @Provides
    @Singleton
    fun provideEnglishWritingStrategy(
        englishWritingUseCase: com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
    ): EnglishWritingStrategy {
        return EnglishWritingStrategy(englishWritingUseCase)
    }
    
    @Provides
    @Singleton
    fun provideFullMemorizationStrategy(
        fullMemorizationUseCase: com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
    ): FullMemorizationStrategy {
        return FullMemorizationStrategy(fullMemorizationUseCase)
    }
    
    @Provides
    @Singleton
    fun provideMemorizationStrategyFactory(
        repeatListeningStrategy: RepeatListeningStrategy,
        englishWritingStrategy: EnglishWritingStrategy,
        fullMemorizationStrategy: FullMemorizationStrategy
    ): MemorizationStrategyFactory {
        return MemorizationStrategyFactory(
            repeatListeningStrategy = repeatListeningStrategy,
            englishWritingStrategy = englishWritingStrategy,
            fullMemorizationStrategy = fullMemorizationStrategy
        )
    }
    
    @Provides
    @Singleton
    fun provideButtonEventHandler(
        ttsController: TtsController,
        strategyFactory: MemorizationStrategyFactory,
        appStateManager: AppStateManager,
        recordingAudioPlayer: RecordingAudioPlayer,
        audioFileManager: AudioFileManager
    ): ButtonEventHandler {
        return ButtonEventHandler(
            ttsController = ttsController,
            strategyFactory = strategyFactory,
            appStateManager = appStateManager,
            recordingAudioPlayer = recordingAudioPlayer,
            audioFileManager = audioFileManager
        )
    }
    
    @Provides
    @Singleton
    fun provideButtonStateManager(): com.na982.opichelper.domain.button.ButtonStateManager {
        return com.na982.opichelper.domain.button.ButtonStateManager()
    }
    
    @Provides
    @Singleton
    fun provideButtonActionHandler(
        buttonStateManager: com.na982.opichelper.domain.button.ButtonStateManager,
        ttsOrchestrator: TtsOrchestrator,
        qaDataRepository: QaDataRepository,
        userPreferencesRepository: UserPreferencesRepository,
        executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase,
        executeRepeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase,
        executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
    ): com.na982.opichelper.domain.button.ButtonActionHandler {
        return com.na982.opichelper.domain.button.ButtonActionHandler(
            buttonStateManager, 
            ttsOrchestrator, 
            qaDataRepository,
            userPreferencesRepository,
            executeFullMemorizationUseCase,
            executeRepeatListeningUseCase,
            executeEnglishWritingTestUseCase
        )
    }
    
    @Provides
    @Singleton
    fun provideInitializeAppUseCase(
        qaDataRepository: QaDataRepository,
        userPreferencesRepository: UserPreferencesRepository,
        appStateManager: AppStateManager
    ): InitializeAppUseCase {
        return InitializeAppUseCase(qaDataRepository, userPreferencesRepository, appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideGetLeveledAnswerUseCase(
        userPreferencesRepository: UserPreferencesRepository
    ): GetLeveledAnswerUseCase {
        return GetLeveledAnswerUseCase(userPreferencesRepository)
    }
    
    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요
} 