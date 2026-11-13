package com.suggestbox.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.suggestbox.BuildConfig
import com.suggestbox.data.local.DraftDao
import com.suggestbox.data.local.RoomDraftRepository
import com.suggestbox.data.local.SuggestBoxDatabase
import com.suggestbox.data.mock.MockAuthRepository
import com.suggestbox.data.mock.MockGovernanceRepository
import com.suggestbox.data.preferences.PreferencesRepositoryImpl
import com.suggestbox.data.remote.email.EmailService
import com.suggestbox.data.remote.email.SmtpEmailService
import com.suggestbox.data.remote.firebase.FirebaseAuthRepository
import com.suggestbox.data.remote.firebase.FirebaseGovernanceRepository
import com.suggestbox.domain.repository.AdminRepository
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.DraftRepository
import com.suggestbox.domain.repository.PreferencesRepository
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt DI module binding local/preferences repositories and selecting between Mock and Firebase
 * backends via [BuildConfig.USE_FIREBASE] without instantiating Firebase when disabled.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(impl: RoomDraftRepository): DraftRepository

    @Binds
    @Singleton
    abstract fun bindEmailService(impl: SmtpEmailService): EmailService
}

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        mockProvider: Provider<MockAuthRepository>,
        firebaseProvider: Provider<FirebaseAuthRepository>,
    ): AuthRepository = if (BuildConfig.USE_FIREBASE) firebaseProvider.get() else mockProvider.get()

    @Provides
    @Singleton
    fun provideSuggestionRepository(
        mockProvider: Provider<MockGovernanceRepository>,
        firebaseProvider: Provider<FirebaseGovernanceRepository>,
    ): SuggestionRepository = if (BuildConfig.USE_FIREBASE) firebaseProvider.get() else mockProvider.get()

    @Provides
    @Singleton
    fun provideAdminRepository(
        mockProvider: Provider<MockGovernanceRepository>,
        firebaseProvider: Provider<FirebaseGovernanceRepository>,
    ): AdminRepository = if (BuildConfig.USE_FIREBASE) firebaseProvider.get() else mockProvider.get()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SuggestBoxDatabase =
        Room.databaseBuilder(
            context,
            SuggestBoxDatabase::class.java,
            "suggestbox_local.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideDraftDao(database: SuggestBoxDatabase): DraftDao = database.draftDao()
}
