package com.na982.opichelper.di

import android.content.Context
import com.na982.opichelper.data.audio.*
import com.na982.opichelper.data.repository.AudioFileRepositoryImpl
import com.na982.opichelper.data.repository.QuestionRepositoryImpl
import com.na982.opichelper.domain.audio.*
import com.na982.opichelper.domain.repository.AudioFileRepository
import com.na982.opichelper.domain.repository.QuestionRepository
import com.na982.opichelper.domain.repository.ProgressRepository
import com.na982.opichelper.domain.repository.ProgressRepositoryImpl
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideAudioFileRepository(@ApplicationContext context: Context): AudioFileRepository {
        return AudioFileRepositoryImpl(context)
    }
    
    // TTS Player (Google TTS를 기본으로 사용)
    @Provides
    @Singleton
    fun provideTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return GoogleTtsPlayer(context)
    }
    
    @Provides
    @Singleton
    fun provideTtsOrchestrator(
        @ApplicationContext context: Context,
        ttsPlayer: TtsPlayer
    ): TtsOrchestrator {
        return TtsOrchestrator(context, ttsPlayer, ttsPlayer)
    }
    
    @Provides
    @Singleton
    fun provideQuestionRepository(@ApplicationContext context: Context): QuestionRepository {
        return QuestionRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideProgressRepository(@ApplicationContext context: Context): ProgressRepository {
        return ProgressRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
    }
    
    // ViewModel들은 @HiltViewModel로 자동 주입되므로 별도 @Provides 불필요
} 