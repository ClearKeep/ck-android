package com.clearkeep.screen.chat.contact_search

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.CKTheme
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.room.RoomActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchUserActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val searchViewModel: SearchViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKTheme {
                SearchUserScreen(
                    searchViewModel,
                    onFinish = { people ->
                        if (people != null) {
                            navigateToRoomScreen(people)
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun navigateToRoomScreen(friend: User) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friend.userId)
        intent.putExtra(RoomActivity.FRIEND_DOMAIN, friend.domain)
        startActivity(intent)
    }
}