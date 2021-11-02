package com.clearkeep.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.screen.auth.forgot.ForgotViewModel
import com.clearkeep.screen.chat.contact_list.PeopleViewModel
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.screen.auth.login.LoginViewModel
import com.clearkeep.screen.auth.register.RegisterViewModel
import com.clearkeep.screen.chat.change_pass_word.ChangePasswordViewModel
import com.clearkeep.screen.chat.group_create.CreateGroupViewModel
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import com.clearkeep.screen.chat.profile.ProfileViewModel
import com.clearkeep.screen.chat.contact_search.SearchViewModel
import com.clearkeep.screen.chat.home.HomeViewModel
import com.clearkeep.screen.chat.notification_setting.NotificationSettingsViewModel
import com.clearkeep.screen.chat.otp.OtpViewModel
import com.clearkeep.screen.chat.settings.ServerSettingViewModel
import com.clearkeep.screen.videojanus.CallViewModel
import com.clearkeep.utilities.BaseViewModel
import com.clearkeep.di.factory.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Suppress("unused")
@InstallIn(SingletonComponent::class)
@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterViewModel::class)
    abstract fun bindRegisterViewModel(registerViewModel: RegisterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ForgotViewModel::class)
    abstract fun bindForgotViewModel(forgotViewModel: ForgotViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PeopleViewModel::class)
    abstract fun bindPeopleViewModel(peopleViewModel: PeopleViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CreateGroupViewModel::class)
    abstract fun bindCreateGroupViewModel(createGroupViewModel: CreateGroupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InviteGroupViewModel::class)
    abstract fun bindInviteGroupViewModel(inviteGroupViewModel: InviteGroupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomViewModel::class)
    abstract fun bindRoomViewModel(roomViewModel: RoomViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(homeViewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ServerSettingViewModel::class)
    abstract fun bindServerSettingViewModel(serverSettingViewModel: ServerSettingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(profileViewModel: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    abstract fun bindSearchViewModel(searchViewModel: SearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CallViewModel::class)
    abstract fun bindCalViewModel(callViewModel: CallViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationSettingsViewModel::class)
    abstract fun bindNotificationSettingsViewModel(notificationSettingsViewModel: NotificationSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OtpViewModel::class)
    abstract fun bindOtpViewModel(otpViewModel: OtpViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseViewModel::class)
    abstract fun bindBaseViewModel(baseViewModel: BaseViewModel): BaseViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChangePasswordViewModel::class)
    abstract fun bindChangePasswordViewModel(changePasswordViewModel: ChangePasswordViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}