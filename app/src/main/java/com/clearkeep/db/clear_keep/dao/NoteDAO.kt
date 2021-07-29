package com.clearkeep.db.clear_keep.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.Note

@Dao
interface NoteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

    @Update
    suspend fun updateNotes(note: Note)

    @Query("SELECT * FROM note WHERE created_time = :createdTime")
    suspend fun getNote(createdTime: Long): Note?

    @Query("SELECT * FROM note WHERE owner_domain = :domain AND owner_client_id = :ownerClientId ORDER BY generateId ASC")
    fun getNotesAsState(domain: String, ownerClientId: String): LiveData<List<Note>>
}