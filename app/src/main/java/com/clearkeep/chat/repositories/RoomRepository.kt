package com.clearkeep.chat.repositories

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
        roomDAO.insert(Room("Peer with $remoteId", remoteId, false))
    }

    fun getGroupRooms() = roomDAO.getGroupRooms()

    fun insertGroupRoom(groupName: String, groupId: String) {
        val roomId = roomDAO.getRoomId(groupId)
        if (roomId != null) {
            return
        }
        roomDAO.insert(Room(groupName, groupId, true))
    }
}