package com.na982.opichelper.di

import android.app.Application
import android.content.Context
import com.na982.opichelper.data.audio.*
import com.na982.opichelper.data.audio.TtsOrchestratorImpl
import com.na982.opichelper.data.audio.TtsPlaybackControllerImpl
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
        @Named("google") googleTtsPlayer: TtsPlayer,
        @Named("samsung") samsungTtsPlayer: TtsPlayer,
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository,
        appLogger: AppLogger
    ): TtsOrchestrator {
        return TtsOrchestratorImpl(googleTtsPlayer, samsungTtsPlayer, userPreferencesRepository, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideProgressPersistenceService(@ApplicationContext context: Context): ProgressPersistenceService {
        return com.na982.opichelper.data.repository.ProgressPersistenceServiceImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataManager(
        qaDataLoader: QaDataLoader,
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository,
        progressPersistenceService: ProgressPersistenceService,
        dataSeeder: DataSeeder
    ): QaDataManager {
        return QaDataManagerImpl(qaDataLoader, userPreferencesRepository, progressPersistenceService, dataSeeder)
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
    
    // WakeLock Controller
    @Provides
    @Singleton
    fun provideWakeLockController(@ApplicationContext context: Context): WakeLockController {
        return WakeLockControllerImpl(context)
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
    fun provideQaDataLoader(dao: QaItemDao): QaDataLoader {
        return RoomQaDataLoader(dao)
    }

    @Provides
    @Singleton
    @Named("asset")
    fun provideAssetQaDataLoader(@ApplicationContext context: Context): QaDataLoader {
        return LeveledQaDataLoader(context)
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
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): DataSeeder = AssetSeeder(qaDataLoader, dao, userPreferencesRepository)
    
    @Provides
    @Singleton
    fun provideEnglishWritingTestRepository(
        ttsOrchestrator: TtsOrchestrator,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressPersistenceService: ProgressPersistenceService
    ): EnglishWritingTestRepository {
        return EnglishWritingTestRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            audioRecorder = audioRecorder,
            audioFileManager = audioFileManager,
            recordingTimeManager = recordingTimeManager,
            progressPersistenceService = progressPersistenceService
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
        progressPersistenceService: ProgressPersistenceService,
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): ScriptEditRepository {
        return ScriptEditRepositoryImpl(dao, recordingTimeManager, progressPersistenceService, userPreferencesRepository)
    }

    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요

    @Provides
    @Singleton
    fun provideTtsPlaybackController(impl: TtsPlaybackControllerImpl): TtsPlaybackController = impl
} 