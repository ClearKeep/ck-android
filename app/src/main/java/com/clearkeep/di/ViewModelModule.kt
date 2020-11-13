package com.clearkeep.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.chat.main.chat.ChatViewModel
import com.clearkeep.chat.main.people.PeopleViewModel
import com.clearkeep.chat.room.RoomViewModel
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
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindGroupChatViewModel(chatViewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PeopleViewModel::class)
    abstract fun bindPeopleViewModel(peopleViewModel: PeopleViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomViewModel::class)
    abstract fun bindRoomViewModel(roomViewModel: RoomViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
