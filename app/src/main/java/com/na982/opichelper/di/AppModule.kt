package com.na982.opichelper.di

import android.content.Context
import com.na982.opichelper.data.audio.*
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import com.na982.opichelper.data.repository.AuthRepository
import com.na982.opichelper.data.repository.QaDataLoaderImpl
import com.na982.opichelper.data.repository.RecordingTimeManagerImpl
import com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl
import com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl
import com.na982.opichelper.domain.audio.*
import com.na982.opichelper.domain.manager.WakeLockManager
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.UserPreferencesRepository
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
    fun provideProgressPersistenceService(@ApplicationContext context: Context): ProgressPersistenceService {
        return com.na982.opichelper.data.repository.ProgressPersistenceServiceImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataManager(
        progressTracker: com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker,
        qaDataLoader: QaDataLoader,
        userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
    ): QaDataManager {
        return QaDataManager(progressTracker, qaDataLoader, userPreferencesRepository)
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
        return com.na982.opichelper.data.repository.UserPreferencesRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataLoader(@ApplicationContext context: Context): QaDataLoader {
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
        ttsOrchestrator: TtsOrchestrator,
        progressTracker: com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker,
        recordingTimeManager: RecordingTimeManager
    ): RepeatListeningRepository {
        return RepeatListeningRepositoryImpl(
            ttsOrchestrator = ttsOrchestrator,
            progressTracker = progressTracker,
            recordingTimeManager = recordingTimeManager
        )
    }

    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요
} 