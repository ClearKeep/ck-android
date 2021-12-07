package com.clearkeep.data.di

import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.data.repository.auth.AuthRepositoryImpl
import com.clearkeep.data.repository.file.FileRepositoryImpl
import com.clearkeep.data.repository.group.GroupRepositoryImpl
import com.clearkeep.data.repository.message.MessageRepositoryImpl
import com.clearkeep.data.repository.notification.NotificationRepositoryImpl
import com.clearkeep.data.repository.people.PeopleRepositoryImpl
import com.clearkeep.data.repository.profile.ProfileRepositoryImpl
import com.clearkeep.data.repository.server.ServerRepositoryImpl
import com.clearkeep.data.repository.signal.SignalKeyRepositoryImpl
import com.clearkeep.data.repository.user.UserRepositoryImpl
import com.clearkeep.data.repository.userkey.UserKeyRepositoryImpl
import com.clearkeep.data.repository.userpreference.UserPreferenceRepositoryImpl
import com.clearkeep.data.repository.videocall.VideoCallRepositoryImpl
import com.clearkeep.data.repository.workspace.WorkSpaceRepositoryImpl
import com.clearkeep.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindGroupRepository(groupRepositoryImpl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    fun bindMessageRepository(messageRepositoryImpl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    fun bindPeopleRepository(peopleRepositoryImpl: PeopleRepositoryImpl): PeopleRepository

    @Binds
    @Singleton
    fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    fun bindServerRepository(serverRepositoryImpl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    fun bindSignalKeyRepository(signalKeyRepositoryImpl: SignalKeyRepositoryImpl): SignalKeyRepository

    @Binds
    @Singleton
    fun bindUserKeyRepository(userKeyRepositoryImpl: UserKeyRepositoryImpl): UserKeyRepository

    @Binds
    @Singleton
    fun bindUserPreferenceRepository(userPreferenceRepositoryImpl: UserPreferenceRepositoryImpl): UserPreferenceRepository

    @Binds
    @Singleton
    fun bindVideoCallRepository(videoCallRepositoryImpl: VideoCallRepositoryImpl): VideoCallRepository

    @Binds
    @Singleton
    fun bindWorkSpaceRepository(workSpaceRepositoryImpl: WorkSpaceRepositoryImpl): WorkSpaceRepository

    @Binds
    @Singleton
    fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindFileRepository(fileRepositoryImpl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    fun bindNotificationRepository(notificationRepository: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    fun bindSenderKeyStore(inMemorySenderKeyStore: InMemorySenderKeyStore): SenderKeyStore

    @Binds
    @Singleton
    fun bindSignalProtocolStore(inMemorySignalProtocolStore: InMemorySignalProtocolStore): SignalProtocolStore

    @Binds
    @Singleton
    fun bindEnvironment(environment: Environment): com.clearkeep.domain.repository.Environment
}