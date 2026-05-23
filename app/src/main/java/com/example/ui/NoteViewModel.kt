package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Note
import com.example.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Dynamic Categories: Predefined list that covers essential use-cases
    val categories = listOf("All", "Work", "Personal", "Ideas", "Tasks", "Shopping", "Others")

    // Clean pastel color palette for notes
    val noteColors = listOf(
        "#FFF59D", // Soft Yellow Page
        "#CE93D8", // Soft Purple Lavender
        "#90CAF9", // Soft Sky Blue
        "#A5D6A7", // Soft Mint Green
        "#FFCC80", // Soft Peach Orange
        "#EF9A9A", // Soft Rose Pink
        "#B2DFDB", // Soft Sage Teal
        "#EEEEEE"  // Minimal Warm Gray
    )

    // Reactive State - Combining database Flow with Search filter & Category selection
    val notesState: StateFlow<List<Note>> = combine(
        repository.allNotes,
        _searchQuery,
        _selectedCategory
    ) { notesList, query, category ->
        notesList.filter { note ->
            val matchesSearch = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            
            val matchesCategory = category == "All" ||
                    note.category.equals(category, ignoreCase = true)
            
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        category: String,
        colorHex: String,
        isPinned: Boolean = false,
        checklistJson: String = ""
    ) {
        viewModelScope.launch {
            val note = Note(
                id = id,
                title = title.trim(),
                content = content.trim(),
                category = category,
                colorHex = colorHex,
                isPinned = isPinned,
                checklistJson = checklistJson,
                timestamp = System.currentTimeMillis()
            )
            if (id == 0) {
                repository.insert(note)
            } else {
                repository.update(note)
            }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun toggleChecklistItem(note: Note, itemIndex: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val items = parseChecklistLines(note.checklistJson).toMutableList()
            if (itemIndex in items.indices) {
                val updatedItem = items[itemIndex].copy(isChecked = isChecked)
                items[itemIndex] = updatedItem
                val serialized = serializeChecklistItems(items)
                repository.update(note.copy(checklistJson = serialized))
            }
        }
    }

    // Secondary actions like toggling checklist in content directly
    fun updateNoteContent(note: Note, newContent: String) {
        viewModelScope.launch {
            repository.update(note.copy(content = newContent, timestamp = System.currentTimeMillis()))
        }
    }

    // Helpers to Parse / Serialize Checklist items
    // Format: Line separated "0|Clean room" or "1|Buy food" where 0=unchecked, 1=checked
    fun parseChecklistLines(raw: String): List<ChecklistItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) {
                val isChecked = parts[0] == "1"
                ChecklistItem(parts[1], isChecked)
            } else null
        }
    }

    fun serializeChecklistItems(items: List<ChecklistItem>): String {
        return items.joinToString("\n") { item ->
            val status = if (item.isChecked) "1" else "0"
            "$status|${item.text}"
        }
    }
}

data class ChecklistItem(val text: String, val isChecked: Boolean)

// Standard factory to provide repository injection
class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
