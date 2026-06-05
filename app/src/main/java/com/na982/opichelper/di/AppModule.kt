package com.na982.opichelper.di

import android.app.Application
import android.content.Context
import com.na982.opichelper.data.audio.*
import com.na982.opichelper.data.audio.TtsOrchestratorImpl
import com.na982.opichelper.data.audio.TtsPlaybackControllerImpl
import com.na982.opichelper.data.usecase.MemorizationModeCoordinatorImpl
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import com.na982.opichelper.data.repository.RecordingTimeManagerImpl
import com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl
import com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl
import com.na982.opichelper.data.repository.ScriptEditRepositoryImpl
import com.na982.opichelper.domain.audio.*
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.manager.WakeLockController
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.data.local.AppDatabase
import com.na982.opichelper.data.local.AssetSeeder
import com.na982.opichelper.data.local.QaItemDao
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.QaDataManagerImpl
import com.na982.opichelper.data.repository.RoomQaDataLoader
import com.na982.opichelper.data.repository.UserPreferencesRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.ScriptEditRepository
import com.na982.opichelper.domain.repository.TtsServiceController
import com.na982.opichelper.domain.repository.DataSeeder
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.data.manager.AndroidLogger
import com.na982.opichelper.data.manager.WakeLockControllerImpl
import com.na982.opichelper.data.repository.RecordingFileRepositoryImpl
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.service.TtsServiceControllerImpl
import com.na982.opichelper.data.audio.RecordingAudioPlayerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Data Layer Implementations
    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context, appLogger: AppLogger): AudioRecorder {
        return AudioRecorderImpl(context, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideAudioPlayer(appLogger: AppLogger): AudioPlayer {
        return AudioPlayerImpl(appLogger)
    }
    
    @Provides
    @Singleton
    fun provideRecordingAudioPlayer(appLogger: AppLogger): RecordingAudioPlayer {
        return RecordingAudioPlayerImpl(appLogger)
    }
    
    @Provides
    @Singleton
    fun provideAudioFileManager(@ApplicationContext context: Context, appLogger: AppLogger): AudioFileManager {
        return AudioFileManagerImpl(context, appLogger)
    }
    
    // TTS Players
    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleTtsPlayer(@ApplicationContext context: Context, appLogger: AppLogger): TtsPlayer {
        return GoogleTtsPlayer(context, appLogger)
    }
    
    @Provides
    @Singleton
    @Named("samsung")
    fun provideSamsungTtsPlayer(@ApplicationContext context: Context, appLogger: AppLogger): TtsPlayer {
        return SamsungTtsPlayer(context, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideTtsOrchestrator(
        @Named("google") googleTtsPlayer: TtsPlayer,
        @Named("samsung") samsungTtsPlayer: TtsPlayer,
        ttsPreferences: com.na982.opichelper.domain.repository.TtsPreferences,
        appLogger: AppLogger
    ): TtsOrchestrator {
        return TtsOrchestratorImpl(googleTtsPlayer, samsungTtsPlayer, ttsPreferences, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideProgressPersistenceService(@ApplicationContext context: Context, appLogger: AppLogger): ProgressPersistenceService {
        return com.na982.opichelper.data.repository.ProgressPersistenceServiceImpl(context, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideQaDataManager(
        qaDataLoader: QaDataLoader,
        userLevelPreferences: com.na982.opichelper.domain.repository.UserLevelPreferences,
        progressPersistenceService: ProgressPersistenceService,
        dataSeeder: DataSeeder,
        appLogger: AppLogger
    ): QaDataManager {
        return QaDataManagerImpl(qaDataLoader, userLevelPreferences, progressPersistenceService, dataSeeder, appLogger)
    }

    @Provides
    @Singleton
    fun provideQaContentReader(manager: QaDataManager): com.na982.opichelper.domain.repository.QaContentReader = manager

    @Provides
    @Singleton
    fun provideQaNavigator(manager: QaDataManager): com.na982.opichelper.domain.repository.QaNavigator = manager

    @Provides
    @Singleton
    fun provideQaSearch(manager: QaDataManager): com.na982.opichelper.domain.repository.QaSearch = manager

    @Provides
    @Singleton
    fun provideQaDataLifecycle(manager: QaDataManager): com.na982.opichelper.domain.repository.QaDataLifecycle = manager

    @Provides
    @Singleton
    fun provideRecordingTimeManager(@ApplicationContext context: Context, appLogger: AppLogger): RecordingTimeManager {
        return RecordingTimeManagerImpl(context, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideRecordingFileRepository(
        audioFileManager: AudioFileManager,
        audioRecorder: AudioRecorder,
        recordingAudioPlayer: RecordingAudioPlayer,
        recordingTimeManager: RecordingTimeManager,
        appLogger: AppLogger
    ): RecordingFileRepository {
        return RecordingFileRepositoryImpl(audioFileManager, audioRecorder, recordingAudioPlayer, recordingTimeManager, appLogger)
    }
    
    // WakeLock Controller
    @Provides
    @Singleton
    fun provideWakeLockController(@ApplicationContext context: Context, appLogger: AppLogger): WakeLockController {
        return WakeLockControllerImpl(context, appLogger)
    }

    // Logger
    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger {
        return AndroidLogger()
    }

    @Provides
    @Singleton
    fun provideTtsServiceController(@ApplicationContext context: Context): TtsServiceController {
        return TtsServiceControllerImpl(context as Application)
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): com.na982.opichelper.domain.repository.UserPreferencesRepository {
        return com.na982.opichelper.data.repository.UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideUserLevelPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.UserLevelPreferences = repo

    @Provides
    @Singleton
    fun provideTtsPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.TtsPreferences = repo

    @Provides
    @Singleton
    fun providePlaybackPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.PlaybackPreferences = repo

    @Provides
    @Singleton
    fun provideOnboardingPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.OnboardingPreferences = repo

    @Provides
    @Singleton
    fun provideMemorizeLevelPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.MemorizeLevelPreferences = repo

    @Provides
    @Singleton
    fun provideAppDataPreferences(
        repo: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.AppDataPreferences = repo
    
    @Provides
    @Singleton
    fun provideQaDataLoader(dao: QaItemDao): QaDataLoader {
        return RoomQaDataLoader(dao)
    }

    @Provides
    @Singleton
    @Named("asset")
    fun provideAssetQaDataLoader(@ApplicationContext context: Context, appLogger: AppLogger): QaDataLoader {
        return LeveledQaDataLoader(context, appLogger)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context, AppDatabase::class.java, "opic_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideQaItemDao(db: AppDatabase): QaItemDao = db.qaItemDao()

    @Provides
    @Singleton
    fun provideDataSeeder(
        @Named("asset") qaDataLoader: QaDataLoader,
        dao: QaItemDao,
        appDataPreferences: com.na982.opichelper.domain.repository.AppDataPreferences
    ): DataSeeder = AssetSeeder(qaDataLoader, dao, appDataPreferences)
    
    @Provides
    @Singleton
    fun provideEnglishWritingTestRepository(
        ttsOrchestrator: TtsOrchestrator,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressPersistenceService: ProgressPersistenceService,
        appLogger: AppLogger
    ): EnglishWritingTestRepository {
        return EnglishWritingTestRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            audioRecorder = audioRecorder,
            audioFileManager = audioFileManager,
            recordingTimeManager = recordingTimeManager,
            progressPersistenceService = progressPersistenceService,
            appLogger = appLogger
        )
    }

    @Provides
    @Singleton
    fun provideRepeatListeningRepository(
        ttsOrchestrator: TtsOrchestrator,
        progressPersistenceService: ProgressPersistenceService,
        recordingTimeManager: RecordingTimeManager
    ): RepeatListeningRepository {
        return RepeatListeningRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            progressPersistenceService = progressPersistenceService,
            recordingTimeManager = recordingTimeManager
        )
    }

    @Provides
    @Singleton
    fun provideScriptEditRepository(
        dao: com.na982.opichelper.data.local.QaItemDao,
        recordingTimeManager: RecordingTimeManager,
        progressPersistenceService: ProgressPersistenceService
    ): ScriptEditRepository {
        return ScriptEditRepositoryImpl(dao, recordingTimeManager, progressPersistenceService)
    }

    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요

    @Provides
    @Singleton
    fun provideMemorizationModeCoordinator(impl: MemorizationModeCoordinatorImpl): MemorizationModeCoordinator = impl

    @Provides
    @Singleton
    fun provideTtsPlaybackController(impl: TtsPlaybackControllerImpl): TtsPlaybackController = impl
} 