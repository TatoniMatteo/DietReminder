package it.matato.dietreminder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.matato.dietreminder.BuildConfig
import it.matato.dietreminder.client.BackendClient
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.data.auth.*
import it.matato.dietreminder.data.repository.contracts.*
import it.matato.dietreminder.data.repository.remote.ServerVersionPolicyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSessionCipher(): SessionCipher = KeystoreSessionCipher()

    @Provides
    @Singleton
    fun provideDietRepository(repository: ScopedAccountRepository): DietRepository = repository

    @Provides
    @Singleton
    fun provideShoppingListRepository(repository: ScopedAccountRepository): ShoppingListRepository = repository

    @Provides
    @Singleton
    fun provideConfigRepository(repository: ScopedAccountRepository): ConfigRepository = repository

    @Provides
    @Singleton
    fun provideVersionPolicyRepository(@ApplicationContext context: Context): VersionPolicyRepository =
        ServerVersionPolicyRepository(context)

    @Provides
    @Singleton
    fun provideDietBackendApiService(sessions: AccountSessionStore): UserService = BackendClient.create(
        BuildConfig.BACKEND_BASE_URL, tokenProvider = sessions::token, onUnauthorized = sessions::unauthorized
    )
}
