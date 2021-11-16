package com.clearkeep.data.di

import com.clearkeep.data.repository.*
import com.clearkeep.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindChatRepository(chatRepositoryImpl: ChatRepositoryImpl): ChatRepository

    @Binds
    fun bindGroupRepository(groupRepositoryImpl: GroupRepositoryImpl): GroupRepository

    @Binds
    fun bindMessageRepository(messageRepositoryImpl: MessageRepositoryImpl): MessageRepository

    @Binds
    fun bindPeopleRepository(peopleRepositoryImpl: PeopleRepositoryImpl): PeopleRepository

    @Binds
    fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    fun bindServerRepository(serverRepositoryImpl: ServerRepositoryImpl): ServerRepository

    @Binds
    fun bindSignalKeyRepository(signalKeyRepositoryImpl: SignalKeyRepositoryImpl): SignalKeyRepository

    @Binds
    fun bindUserKeyRepository (userKeyRepositoryImpl: UserKeyRepositoryImpl): UserKeyRepository

    @Binds
    fun bindUserPreferenceRepository (userPreferenceRepositoryImpl: UserPreferenceRepositoryImpl): UserPreferenceRepository

    @Binds
    fun bindVideoCallRepository (videoCallRepositoryImpl: VideoCallRepositoryImpl): VideoCallRepository

    @Binds
    fun bindWorkSpaceRepository (workSpaceRepositoryImpl: WorkSpaceRepositoryImpl): WorkSpaceRepository

    @Binds
    fun bindAuthRepository (authRepositoryImpl: AuthRepositoryImpl): AuthRepository
}