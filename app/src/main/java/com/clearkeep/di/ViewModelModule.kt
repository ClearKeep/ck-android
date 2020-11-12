package com.clearkeep.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.chat.group.GroupChatViewModel
import com.clearkeep.chat.single.PeerChatViewModel
import com.clearkeep.login.LoginViewModel
import com.setel.di.ViewModelKey
import com.setel.di.factory.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.multibindings.IntoMap

@Suppress("unused")
@InstallIn(ApplicationComponent::class)
@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PeerChatViewModel::class)
    abstract fun bindSingleChatViewModel(singleChatViewModel: PeerChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GroupChatViewModel::class)
    abstract fun bindGroupChatViewModel(groupChatViewModel: GroupChatViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
