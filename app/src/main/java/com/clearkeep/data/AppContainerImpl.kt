package com.clearkeep.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.clearkeep.db.UserDatabase
import com.clearkeep.db.UserLocalDataSource
import com.clearkeep.db.UserRepository
import grpc.PscrudGrpc
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking


/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val grpcClient: PscrudGrpc.PscrudStub

    //    val sharedPreferences: SharedPreferences
    val mainThreadHandler: Handler
    val dbLocal: UserRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class AppContainerImpl(context: Context) : AppContainer {
    override val grpcClient: PscrudGrpc.PscrudStub by lazy {
        val channel = ManagedChannelBuilder.forAddress("jetpack.tel.red", 11912)
//        val channel = ManagedChannelBuilder.forAddress("10.0.2.2", 11912)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        PscrudGrpc.newStub(channel)
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
}




