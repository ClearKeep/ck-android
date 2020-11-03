package com.clearkeep.chat.repo

import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Room
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomDAO: RoomDAO
) {
    fun getSingleRooms() = roomDAO.getSingleRooms()

    fun insertSingleRoom(remoteId: String) {
        roomDAO.insert(Room(remoteId, false))
    }
}