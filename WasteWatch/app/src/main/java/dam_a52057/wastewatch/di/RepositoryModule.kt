package dam_a52057.wastewatch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dam_a52057.wastewatch.data.repository.AuthRepository
import dam_a52057.wastewatch.data.repository.FirebaseAuthRepository
import dam_a52057.wastewatch.data.repository.FirestoreSocialRepository
import dam_a52057.wastewatch.data.repository.SocialRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(
        impl: FirestoreSocialRepository
    ): SocialRepository
}
