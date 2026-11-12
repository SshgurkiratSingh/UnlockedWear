package com.unlockedwear.mobile.ai

import com.unlockedwear.mobile.data.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing AI engine clients and the intelligence router.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideDeepSeekClient(
        appPreferences: AppPreferences
    ): DeepSeekClient {
        return DeepSeekClient(appPreferences)
    }

    @Provides
    @Singleton
    fun provideGeminiClient(
        appPreferences: AppPreferences
    ): GeminiClient {
        return GeminiClient(appPreferences)
    }

    @Provides
    @Singleton
    fun provideIntelligenceRouter(
        deepSeekClient: DeepSeekClient,
        geminiClient: GeminiClient
    ): IntelligenceRouter {
        return IntelligenceRouter(deepSeekClient, geminiClient)
    }
}
