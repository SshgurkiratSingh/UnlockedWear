package com.unlockedwear.mobile.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Wearable Data Layer clients and phone-to-watch synchronization service.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideDataClient(@ApplicationContext context: Context): DataClient {
        return Wearable.getDataClient(context)
    }

    @Provides
    @Singleton
    fun provideMessageClient(@ApplicationContext context: Context): MessageClient {
        return Wearable.getMessageClient(context)
    }

    @Provides
    @Singleton
    fun provideNodeClient(@ApplicationContext context: Context): NodeClient {
        return Wearable.getNodeClient(context)
    }

    @Provides
    @Singleton
    fun providePhoneToWatchSyncer(
        @ApplicationContext context: Context,
        dataClient: DataClient,
        messageClient: MessageClient,
        nodeClient: NodeClient
    ): PhoneToWatchSyncer {
        return PhoneToWatchSyncer(context, dataClient, messageClient, nodeClient)
    }
}
