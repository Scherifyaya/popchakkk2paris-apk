package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%")
    }

    fun getNotesByCategory(category: String): Flow<List<Note>> {
        return noteDao.getNotesByCategory(category)
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteById(id: Int) {
        noteDao.deleteNoteById(id)
    }
}
