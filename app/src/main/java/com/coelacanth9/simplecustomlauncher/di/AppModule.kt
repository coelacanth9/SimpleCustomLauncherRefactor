package com.coelacanth9.simplecustomlauncher.di

import android.content.Context
import com.coelacanth9.simplecustomlauncher.data.SettingsRepository
import com.coelacanth9.simplecustomlauncher.data.ShortcutRepository
import com.coelacanth9.simplecustomlauncher.platform.BackupManager
import com.coelacanth9.simplecustomlauncher.platform.ShortcutHelper
import com.coelacanth9.simplecustomlauncher.platform.ads.AdManager
import com.coelacanth9.simplecustomlauncher.platform.billing.BillingManager
import com.coelacanth9.simplecustomlauncher.platform.billing.DefaultPremiumManager
import com.coelacanth9.simplecustomlauncher.platform.billing.PremiumManager
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
    fun provideShortcutRepository(@ApplicationContext context: Context): ShortcutRepository =
        ShortcutRepository(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun providePremiumManager(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): PremiumManager = DefaultPremiumManager(context, settingsRepository)

    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager =
        BillingManager(
            context = context,
            onPurchaseComplete = {},
            onPurchaseCleared = {}
        )

    @Provides
    @Singleton
    fun provideAdManager(@ApplicationContext context: Context): AdManager =
        AdManager(context)

    @Provides
    @Singleton
    fun provideShortcutHelper(@ApplicationContext context: Context): ShortcutHelper =
        ShortcutHelper(context)

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        shortcutRepository: ShortcutRepository,
        settingsRepository: SettingsRepository
    ): BackupManager = BackupManager(context, shortcutRepository, settingsRepository)
}
