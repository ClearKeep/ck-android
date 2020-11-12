package com.clearkeep.chat.repositories

import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Room
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomDAO: RoomDAO
) {
    fun getRoom(roomId: Int) = roomDAO.getRoomFromIdAsState(roomId)

    fun getPeerRooms() = roomDAO.getPeerRooms()

    fun insertPeerRoom(remoteId: String) {
        roomDAO.insert(Room("$remoteId", remoteId, false))
    }

    fun getGroupRooms() = roomDAO.getGroupRooms()

    fun remarkJoinInRoom(roomId: Int) : Boolean {
        val room = roomDAO.getRoomFromId(roomId)
        val newRoom = Room(room.roomName, room.remoteId, room.isGroup, isAccepted = true)
        newRoom.id = room.id
        roomDAO.update(newRoom)
        return true
    }

    fun insertGroupRoom(groupName: String, groupId: String) {
        val roomId = roomDAO.getRoomId(groupId)
        if (roomId != null) {
            return
        }
        roomDAO.insert(Room(groupName, groupId, true, isAccepted = false))
    }
}