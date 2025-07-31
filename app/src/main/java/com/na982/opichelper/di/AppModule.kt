package com.na982.opichelper.di

import android.content.Context
import com.na982.opichelper.data.audio.*
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import com.na982.opichelper.data.repository.AuthRepository
import com.na982.opichelper.data.repository.QaDataLoaderImpl
import com.na982.opichelper.data.repository.RecordingTimeManagerImpl
import com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl
import com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl
import com.na982.opichelper.data.repository.FullMemorizationRepositoryImpl
import com.na982.opichelper.domain.audio.*
import com.na982.opichelper.domain.manager.WakeLockManager
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.UserPreferencesRepositoryImpl
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.data.repository.RecordingFileRepositoryImpl
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.data.audio.RecordingAudioPlayerImpl
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named
import com.na982.opichelper.domain.usecase.InitializeAppUseCase
import com.na982.opichelper.domain.usecase.GetCurrentAnswerUseCase
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.state.StateManager
import com.na982.opichelper.domain.event.ButtonEventHandler

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
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
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
    fun provideQaDataManager(
        leveledQaDataLoader: LeveledQaDataLoader,
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): QaDataManager {
        return QaDataManager(leveledQaDataLoader, userPreferencesRepository)
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
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        return AuthRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): com.na982.opichelper.domain.repository.UserPreferencesRepository {
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
        qaDataManager: QaDataManager,
        ttsOrchestrator: TtsOrchestrator,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressTracker: com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
    ): EnglishWritingTestRepository {
        return EnglishWritingTestRepositoryImpl(
            qaDataManager = qaDataManager,
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
        repeatListeningService: com.na982.opichelper.domain.usecase.RepeatListeningService,
        progressTracker: com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
    ): RepeatListeningRepository {
        return RepeatListeningRepositoryImpl(
            repeatListeningService = repeatListeningService,
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
        qaDataManager: QaDataManager
    ): FullMemorizationRepository {
        return FullMemorizationRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            audioRecorder = audioRecorder,
            audioPlayer = audioPlayer,
            audioFileManager = audioFileManager,
            qaDataManager = qaDataManager
        )
    }
    
    // 새로운 버튼 관리 클래스들
    @Provides
    @Singleton
    fun provideButtonStateManager(
        appStateManager: com.na982.opichelper.domain.state.AppStateManager
    ): com.na982.opichelper.domain.audio.ButtonStateManager {
        return com.na982.opichelper.domain.audio.ButtonStateManager(appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideInterruptManager(
        buttonStateManager: com.na982.opichelper.domain.audio.ButtonStateManager,
        ttsOrchestrator: TtsOrchestrator
    ): com.na982.opichelper.domain.audio.InterruptManager {
        return com.na982.opichelper.domain.audio.InterruptManager(buttonStateManager, ttsOrchestrator)
    }
    
    @Provides
    @Singleton
    fun provideButtonStateObserver(ttsOrchestrator: TtsOrchestrator): com.na982.opichelper.domain.audio.ButtonStateObserver {
        return ttsOrchestrator
    }
    
    @Provides
    @Singleton
    fun provideButtonStateCoordinator(
        buttonStateManager: com.na982.opichelper.domain.audio.ButtonStateManager,
        buttonStateObserver: com.na982.opichelper.domain.audio.ButtonStateObserver
    ): com.na982.opichelper.domain.audio.ButtonStateCoordinator {
        return com.na982.opichelper.domain.audio.ButtonStateCoordinator(buttonStateManager, buttonStateObserver)
    }
    
    @Provides
    @Singleton
    fun provideAppStateManager(): com.na982.opichelper.domain.state.AppStateManager {
        return com.na982.opichelper.domain.state.AppStateManager()
    }
    
    @Provides
    @Singleton
    fun provideRepeatListeningService(
        ttsController: com.na982.opichelper.domain.audio.TtsController,
        progressTracker: com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker,
        recordingTimeManager: RecordingTimeManager
    ): com.na982.opichelper.domain.usecase.RepeatListeningService {
        return com.na982.opichelper.domain.usecase.RepeatListeningService(
            ttsController = ttsController,
            progressTracker = progressTracker,
            recordingTimeManager = recordingTimeManager
        )
    }
    

    
    @Provides
    @Singleton
    fun provideTtsController(
        ttsOrchestrator: TtsOrchestrator,
        appStateManager: com.na982.opichelper.domain.state.AppStateManager
    ): com.na982.opichelper.domain.audio.TtsController {
        return com.na982.opichelper.data.audio.TtsControllerImpl(
            ttsOrchestrator = ttsOrchestrator,
            appStateManager = appStateManager
        )
    }
    
    @Provides
    @Singleton
    fun provideStateManager(appStateManager: AppStateManager): StateManager {
        return appStateManager
    }
    
    @Provides
    @Singleton
    fun provideButtonEventHandler(
        ttsController: TtsController,
        repeatListeningService: com.na982.opichelper.domain.usecase.RepeatListeningService,
        executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase,
        executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.ExecuteFullMemorizationUseCase,
        stateManager: StateManager
    ): ButtonEventHandler {
        return ButtonEventHandler(
            ttsController = ttsController,
            repeatListeningService = repeatListeningService,
            executeEnglishWritingTestUseCase = executeEnglishWritingTestUseCase,
            executeFullMemorizationUseCase = executeFullMemorizationUseCase,
            stateManager = stateManager
        )
    }
    
    @Provides
    @Singleton
    fun provideButtonActionHandler(
        buttonStateManager: com.na982.opichelper.domain.audio.ButtonStateManager,
        ttsOrchestrator: TtsOrchestrator,
        interruptManager: com.na982.opichelper.domain.audio.InterruptManager,
        qaDataManager: QaDataManager,
        executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.ExecuteFullMemorizationUseCase,
        executeRepeatListeningUseCase: com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase,
        executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
    ): com.na982.opichelper.domain.audio.ButtonActionHandler {
        return com.na982.opichelper.domain.audio.ButtonActionHandler(
            buttonStateManager, 
            ttsOrchestrator, 
            interruptManager, 
            qaDataManager,
            executeFullMemorizationUseCase,
            executeRepeatListeningUseCase,
            executeEnglishWritingTestUseCase
        )
    }
    
    @Provides
    @Singleton
    fun provideInitializeAppUseCase(
        qaDataManager: QaDataManager,
        userPreferencesRepository: UserPreferencesRepository,
        appStateManager: AppStateManager
    ): InitializeAppUseCase {
        return InitializeAppUseCase(qaDataManager, userPreferencesRepository, appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideGetCurrentAnswerUseCase(
        userPreferencesRepository: UserPreferencesRepository
    ): GetCurrentAnswerUseCase {
        return GetCurrentAnswerUseCase(userPreferencesRepository)
    }
    
    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요
} 