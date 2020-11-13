package com.clearkeep.chat.repositories

import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Room
import com.clearkeep.login.LoginRepository
import com.clearkeep.utilities.getCurrentDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomDAO: RoomDAO,
) {
    suspend fun getRoom(roomId: Int) = roomDAO.getRoomFromId(roomId)

    private suspend fun insertPeerRoom(clientId: String, remoteId: String) : Int {
        val room = Room(
                roomName = remoteId,
                remoteId = remoteId,
                isGroup = false,

                lastPeople = clientId,
                lastMessage = "$clientId send message",
                lastUpdatedTime = getCurrentDateTime().time,
                isRead = false,
        )
        return roomDAO.insert(room).toInt()
    }

    private suspend fun insertGroupRoom(clientId: String, groupName: String, groupId: String) {
        val oldRoom = roomDAO.getRoomFromRemoteId(groupId)
        if (oldRoom != null) {
            return
        }

        val room = Room(
            roomName = groupName,
            remoteId = groupId,
            isGroup = true,
            isAccepted = false,

            lastPeople = clientId,
            lastMessage = "$clientId created group",
            lastUpdatedTime = getCurrentDateTime().time,
            isRead = false,
        )
        roomDAO.insert(room)
    }

    fun getAllRooms() = roomDAO.getRooms()

    private suspend fun getRoomFromRemoteId(remoteId: String) = roomDAO.getRoomFromRemoteId(remoteId)

    suspend fun getRoomOrCreateIfNot(clientId: String, remoteId: String, isGroup: Boolean) : Room {
        var room = getRoomFromRemoteId(remoteId)
        if (room == null) {
            if (!isGroup) {
                insertPeerRoom(clientId, remoteId)
            } else {
                insertGroupRoom(clientId, remoteId, remoteId)
            }
        }
        return getRoomFromRemoteId(remoteId)
    }

    suspend fun remarkJoinInRoom(roomId: Int) : Boolean {
        val room = roomDAO.getRoomFromId(roomId)
        val updateRoom = Room(
                id = room.id,
                roomName = room.roomName,
                remoteId = room.remoteId,
                isGroup = room.isGroup,
                isAccepted = true,

                lastPeople = room.lastPeople,
                lastMessage = room.lastMessage,
                lastUpdatedTime = room.lastUpdatedTime,
                isRead = room.isRead,
        )
        roomDAO.update(updateRoom)
        return true
    }

    suspend fun updateRoom(room: Room) = roomDAO.update(room)
}