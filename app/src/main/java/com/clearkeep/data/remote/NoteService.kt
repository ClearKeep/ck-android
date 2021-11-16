package com.clearkeep.data.remote

import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import note.NoteOuterClass
import javax.inject.Inject

class NoteService@Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val paramAPIProvider: ParamAPIProvider,
) {
    suspend fun getNotes(server: Server): NoteOuterClass.GetUserNotesResponse = withContext(Dispatchers.IO) {
        val notesGrpc = paramAPIProvider.provideNotesBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        val request = NoteOuterClass.GetUserNotesRequest.newBuilder().build()
        return@withContext notesGrpc.getUserNotes(request)
    }

    suspend fun sendNote(content: String): NoteOuterClass.UserNoteResponse = withContext(Dispatchers.IO) {
        val request = NoteOuterClass.CreateNoteRequest.newBuilder()
            .setTitle("")
            .setContent(ByteString.copyFrom(content, Charsets.UTF_8))
            .setNoteType("")
            .build()
        return@withContext dynamicAPIProvider.provideNoteBlockingStub().createNote(request)
    }
}