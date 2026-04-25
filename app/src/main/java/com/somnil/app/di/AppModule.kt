package com.somnil.app.di

import android.content.Context
import androidx.room.Room
import com.somnil.app.data.local.SleepSessionDao
import com.somnil.app.data.local.SomnilDatabase
import com.somnil.app.network.SomnilRepository
import com.somnil.app.service.*
import com.somnil.app.domain.model.TrainingPhase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBLEManager(
        @ApplicationContext context: Context
    ): BLEManager = BLEManager(context)

    @Provides
    @Singleton
    fun provideDataProcessor(
        bleManager: BLEManager
    ): DataProcessor = DataProcessor(bleManager)

    @Provides
    @Singleton
    fun provideHealthConnectManager(
        @ApplicationContext context: Context
    ): HealthConnectManager = HealthConnectManager(context)

    @Provides
    @Singleton
    fun provideMQTTManager(): MQTTManager = MQTTManager()

    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context
    ): AudioPlayerManager = AudioPlayerManager(context)

    @Provides
    @Singleton
    fun provideTrainingDataStore(
        @ApplicationContext context: Context
    ): TrainingDataStore = TrainingDataStore(context)

    @Provides
    @Singleton
    fun providePressureHistoryManager(
        @ApplicationContext context: Context
    ): PressureHistoryManager = PressureHistoryManager(context)

    @Provides
    @Singleton
    fun provideSomnilRepository(): SomnilRepository = SomnilRepository()

    @Provides
    @Singleton
    fun provideSomnilDatabase(
        @ApplicationContext context: Context
    ): SomnilDatabase = Room.databaseBuilder(
        context,
        SomnilDatabase::class.java,
        "somnil_database"
    ).build()

    @Provides
    @Singleton
    fun provideSleepSessionDao(database: SomnilDatabase): SleepSessionDao = database.sleepSessionDao()
}