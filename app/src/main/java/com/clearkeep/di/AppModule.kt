package com.clearkeep.di

import android.text.TextUtils
import android.util.Base64
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.utilities.storage.Storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.util.KeyHelper
import signalc.SignalKeyDistributionGrpc
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, 50051)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newStub(channel)
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, 50051)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newBlockingStub(channel)
    }

    @Singleton
    @Provides
    fun provideInMemorySignalProtocolStore(storage: Storage): InMemorySignalProtocolStore {
        val identityKeyPairStr = storage.getString("signal_identity_key")
        var identityKeyPair: IdentityKeyPair
        if (!TextUtils.isEmpty(identityKeyPairStr)) {
            val array: ByteArray = Base64.decode(identityKeyPairStr, Base64.DEFAULT)
            identityKeyPair = IdentityKeyPair(array)
        } else {
            identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val saveThis = Base64.encodeToString(identityKeyPair.serialize(), Base64.DEFAULT)
            storage.setString("signal_identity_key", saveThis)
        }

        var registrationID = storage.getInt("signal_registration_id")
        if (registrationID == -1) {
            registrationID = KeyHelper.generateRegistrationId(false)
            storage.setInt("signal_registration_id", registrationID)
        }

        return InMemorySignalProtocolStore(identityKeyPair, registrationID, storage)
    }

    companion object {
        private const val BASE_URL = "172.16.1.41"
    }
}
