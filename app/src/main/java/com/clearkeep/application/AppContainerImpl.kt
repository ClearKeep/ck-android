package com.clearkeep.application

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.clearkeep.db.UserDatabase
import com.clearkeep.db.UserLocalDataSource
import com.clearkeep.db.UserRepository
import com.clearkeep.store.InMemorySignalProtocolStore
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.whispersystems.libsignal.util.KeyHelper
import signalc.SignalKeyDistributionGrpc


/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub

    //    val sharedPreferences: SharedPreferences
    val mainThreadHandler: Handler
    val dbLocal: UserRepository
    val myStore: InMemorySignalProtocolStore
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class AppContainerImpl(context: Context) :
    AppContainer {
    override val grpcClient: SignalKeyDistributionGrpc.SignalKeyDistributionStub by lazy {
//        val channel = ManagedChannelBuilder.forAddress("jetpack.tel.red", 11912)
        val channel = ManagedChannelBuilder.forAddress("172.16.0.216", 50051)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        SignalKeyDistributionGrpc.newStub(channel)
    }

//    override val sharedPreferences: SharedPreferences by lazy {
//        context.getSharedPreferences("CK_SHARED_PREF", Context.MODE_PRIVATE)
//    }

    override val mainThreadHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override val dbLocal: UserRepository by lazy {
        runBlocking {
            val userDatabase = async { UserDatabase.getInstance(context) }.await()
            val userLocal =
                async { UserLocalDataSource.getInstance(userDatabase!!.userDAO()) }.await()
            val userRepository = async { UserRepository.getInstance(userLocal) }.await()
            return@runBlocking userRepository!!
        }
    }


    override val myStore: InMemorySignalProtocolStore by lazy {
        val identityKeyPair = KeyHelper.generateIdentityKeyPair()
        val registrationID = KeyHelper.generateRegistrationId(false)
        InMemorySignalProtocolStore(identityKeyPair, registrationID)
    }
}




