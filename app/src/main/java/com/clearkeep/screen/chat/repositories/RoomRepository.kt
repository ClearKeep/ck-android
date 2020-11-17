package com.clearkeep.screen.chat.repositories

import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Room
import com.clearkeep.utilities.getCurrentDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomDAO: RoomDAO,
) {
    suspend fun getRoom(roomId: Int) = roomDAO.getRoomById(roomId)

    suspend fun insertPeerRoom(lastCliendId: String, remoteId: String) : Int {
        val room = Room(
                roomName = remoteId,
                remoteId = remoteId,
                isGroup = false,

                lastPeople = lastCliendId,
                lastMessage = "$lastCliendId send message",
                lastUpdatedTime = getCurrentDateTime().time,
                isRead = false,
        )
        return roomDAO.insert(room).toInt()
    }

    suspend fun insertGroupRoom(lastClientId: String, groupName: String, groupId: String) {
        val oldRoom = roomDAO.getRoomByRemoteId(groupId)
        if (oldRoom != null) {
            return
        }

        val room = Room(
            roomName = groupName,
            remoteId = groupId,
            isGroup = true,
            isAccepted = false,

            lastPeople = lastClientId,
            lastMessage = "$lastClientId created group",
            lastUpdatedTime = getCurrentDateTime().time,
            isRead = false,
        )
        roomDAO.insert(room)
    }

    suspend fun getRoomByRemoteId(remoteId: String) = roomDAO.getRoomByRemoteId(remoteId)

    suspend fun remarkJoinInRoom(roomId: Int) : Boolean {
        val room = roomDAO.getRoomById(roomId)
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

    fun getAllRooms() = roomDAO.getRooms()
}