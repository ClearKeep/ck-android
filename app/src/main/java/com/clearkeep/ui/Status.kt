/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clearkeep.ui

import androidx.compose.Model

/**
 * Class defining the screens we have in the app: home, article details and interests
 */
sealed class Screen {
    object Home : Screen()
    object HomeView2 : Screen()
    object CreateNewRoom : Screen()
    data class RoomDetail(val roomId: String) : Screen()
}

@Model
object ChatStatus {
    var currentScreen: Screen = Screen.Home
}

/**
 * Temporary solution pending navigation support.
 */
fun navigateTo(destination: Screen) {
    ChatStatus.currentScreen = destination
}
