package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.ui.ChecklistItem
import com.example.ui.NoteViewModel
import com.example.ui.NoteViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database layers
        val database = AppDatabase.getDatabase(this)
        val repository = NoteRepository(database.noteDao())
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Dependency Injection via Custom Factory
                val viewModel: NoteViewModel = viewModel(
                    factory = NoteViewModelFactory(repository)
                )
                
                NotebookDashboard(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotebookDashboard(viewModel: NoteViewModel) {
    val notes by viewModel.notesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    // UI state states for creating / details sheets
    var showAddEditDialog by remember { mutableStateOf<Note?>(null) }
    var activeDetailsNote by remember { mutableStateOf<Note?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Count pinned and total tasks for summary statistics
    val pinnedCount = notes.count { it.isPinned }
    val totalCount = notes.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isCreatingNew = true
                    showAddEditDialog = Note(
                        title = "",
                        content = "",
                        category = "Work",
                        colorHex = viewModel.noteColors.first()
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_note_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Header with App Title and Statistics Block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "POPCHAKK2PARIS",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mon Carnet de Notes & Tâches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Notebook Symbol Illustration
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "Notebook Pattern icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Simple working statistics bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Épinglées",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$pinnedCount",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Épinglées",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = "Total",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$totalCount",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Notes au total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search bar input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Rechercher une note ou tâche...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Recherche") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally Scrollable Category list choice chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(viewModel.categories) { categoryName ->
                    val isSelected = selectedCategory == categoryName
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(categoryName) },
                        label = {
                            Text(
                                text = if (categoryName == "All") "Tout" else categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes list display layout grid (combines Pinned and Unpinned seamlessly)
            if (notes.isEmpty()) {
                // Empty state illustration drawing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Aucune note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Aucun résultat pour \"$searchQuery\"" else "Votre carnet est vide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Veuillez modifier votre terme de recherche" else "Ajoutez des notes ou des listes de tâches en appuyant sur le bouton + !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Split pinned and standard items visually if no manual category filter
                    val pinnedNotes = notes.filter { it.isPinned }
                    val standardNotes = notes.filter { !it.isPinned }

                    if (pinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "ÉPINGLÉES 📌",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteGridCard(
                                note = note,
                                viewModel = viewModel,
                                onClick = { activeDetailsNote = note },
                                onPinToggle = { viewModel.togglePin(note) }
                            )
                        }
                    }

                    if (standardNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            val headerText = if (pinnedNotes.isNotEmpty()) "AUTRES NOTES" else "TOUTES LES NOTES"
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(standardNotes, key = { it.id }) { note ->
                            NoteGridCard(
                                note = note,
                                viewModel = viewModel,
                                onClick = { activeDetailsNote = note },
                                onPinToggle = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for adding or editing notes/tasks
    showAddEditDialog?.let { note ->
        AddEditNoteDialog(
            note = note,
            viewModel = viewModel,
            isNew = isCreatingNew,
            onDismiss = { showAddEditDialog = null },
            onSave = { title, content, category, colorHex, isPinned, checklistJson ->
                viewModel.saveNote(
                    id = note.id,
                    title = title,
                    content = content,
                    category = category,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    checklistJson = checklistJson
                )
                showAddEditDialog = null
            }
        )
    }

    // Modal Dialog to display rich Note Details and interactive checkboxes
    activeDetailsNote?.let { note ->
        NoteDetailDialog(
            note = note,
            viewModel = viewModel,
            onDismiss = { activeDetailsNote = null },
            onEdit = {
                isCreatingNew = false
                showAddEditDialog = note
                activeDetailsNote = null
            },
            onDelete = {
                viewModel.deleteNote(note)
                activeDetailsNote = null
            },
            onPinToggle = {
                viewModel.togglePin(note)
                // update current dialog overlay state reflectively
                activeDetailsNote = note.copy(isPinned = !note.isPinned)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridCard(
    note: Note,
    viewModel: NoteViewModel,
    onClick: () -> Unit,
    onPinToggle: () -> Unit
) {
    val noteColor = Color(android.graphics.Color.parseColor(note.colorHex))
    val contrastOnColor = if (note.colorHex == "#EEEEEE") Color.Black else Color(0xFF2E2E2E)
    val subtitleColor = contrastOnColor.copy(alpha = 0.7f)
    
    val checklistItems = remember(note.checklistJson) {
        viewModel.parseChecklistLines(note.checklistJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 220.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onPinToggle
            )
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header of card: Category Tag + Pin Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(contrastOnColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = contrastOnColor
                        )
                    }
                    
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Pin indicator",
                            tint = if (note.isPinned) Color(0xFFFBC02D) else contrastOnColor.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title details
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = contrastOnColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Body text or Checklist Quick Previews!
                if (checklistItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Show max 3 checklist items on the card preview
                        checklistItems.take(3).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = contrastOnColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color.Transparent, CircleShape)
                                            .border(1.2.dp, contrastOnColor.copy(alpha = 0.6f), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.isChecked) subtitleColor.copy(alpha = 0.6f) else subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                )
                            }
                        }
                        if (checklistItems.size > 3) {
                            Text(
                                text = "+ ${checklistItems.size - 3} éléments de plus",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer of card: Nice readable clock
            val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
            val formattedDate = formatter.format(Date(note.timestamp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteDialog(
    note: Note,
    viewModel: NoteViewModel,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, colorHex: String, isPinned: Boolean, checklistJson: String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var category by remember { mutableStateOf(note.category) }
    var colorHex by remember { mutableStateOf(note.colorHex) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    
    // Checklist creation state
    var checklistItems by remember { 
        mutableStateOf(viewModel.parseChecklistLines(note.checklistJson)) 
    }
    var newChecklistItemText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp) // Offset for system status icons
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (isNew) "Créer une note" else "Modifier la note") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Clear, contentDescription = "Annuler")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    onSave(
                                        title,
                                        content,
                                        category,
                                        colorHex,
                                        isPinned,
                                        viewModel.serializeChecklistItems(checklistItems)
                                    )
                                },
                                modifier = Modifier.testTag("save_note_button"),
                                enabled = title.isNotBlank() || content.isNotBlank() || checklistItems.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Confirmer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { contentPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Style Accent Note Color Strip
                    item {
                        Column {
                            Text(
                                "Couleur d'arrière-plan de la note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(viewModel.noteColors) { hex ->
                                    val itemColor = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(itemColor)
                                            .clickable { colorHex = hex }
                                            .testTag("color_picker_$hex"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (colorHex == hex) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF2E2E2E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category Selection Chips
                    item {
                        Column {
                            Text(
                                "Catégorie",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.categories.filter { it != "All" }.forEach { catName ->
                                    val isPicked = category == catName
                                    FilterChip(
                                        selected = isPicked,
                                        onClick = { category = catName },
                                        label = { Text(catName) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick pin utility
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Pin toggler Icon",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Épingler en haut de la liste",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = isPinned,
                                    onCheckedChange = { isPinned = it }
                                )
                            }
                        }
                    }

                    // Text Input Fields for Note Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Titre de la note") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_title_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Checklist or Note Content Decider Tabs
                    item {
                        var isChecklistMode by remember { mutableStateOf(checklistItems.isNotEmpty()) }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { isChecklistMode = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isChecklistMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isChecklistMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Texte Libre")
                                }

                                Button(
                                    onClick = { isChecklistMode = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChecklistMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isChecklistMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tâches / Checklist")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isChecklistMode) {
                                // Checklist builder widget
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "Liste de Tâches",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Add task field
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newChecklistItemText,
                                            onValueChange = { newChecklistItemText = it },
                                            placeholder = { Text("Faire la vaisselle, etc.") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (newChecklistItemText.isNotBlank()) {
                                                    checklistItems = checklistItems + ChecklistItem(newChecklistItemText, false)
                                                    newChecklistItemText = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .size(44.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Displayed task checklist entries
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        checklistItems.forEachIndexed { index, item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = item.isChecked,
                                                        onCheckedChange = { isChecked ->
                                                            val updatedList = checklistItems.toMutableList()
                                                            updatedList[index] = item.copy(isChecked = isChecked)
                                                            checklistItems = updatedList
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = item.text,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Supprimer l'élément",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Full note body text area field
                                OutlinedTextField(
                                    value = content,
                                    onValueChange = { content = it },
                                    placeholder = { Text("Écrivez vos pensées, notes ou textes ici...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 500.dp)
                                        .testTag("note_content_input"),
                                    shape = RoundedCornerShape(16.dp),
                                    maxLines = 15
                                )
                            }
                        }
                    }

                    // Spacer bottom for comfortable layout scrolling
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

// Fullscreen-like visual Dialog displaying deep Note details, allowing sharing, deleting,
// or completing checklist items interactively inside the dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailDialog(
    note: Note,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit
) {
    val noteColor = Color(android.graphics.Color.parseColor(note.colorHex))
    val contrastOnColor = if (note.colorHex == "#EEEEEE") Color.Black else Color(0xFF2E2E2E)
    val subtitleColor = contrastOnColor.copy(alpha = 0.7f)
    
    val context = LocalContext.current
    var checklistItems by remember(note.checklistJson) {
        mutableStateOf(viewModel.parseChecklistLines(note.checklistJson))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = noteColor
        ) {
            Scaffold(
                containerColor = noteColor,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = note.category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = contrastOnColor
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Clear, contentDescription = "Fermer", tint = contrastOnColor)
                            }
                        },
                        actions = {
                            // Pin note action
                            IconButton(onClick = onPinToggle) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Épingler",
                                    tint = if (note.isPinned) Color(0xFFFBC02D) else contrastOnColor.copy(alpha = 0.3f)
                                )
                            }
                            
                            // Share note action
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine(note.title)
                                        appendLine("---")
                                        if (checklistItems.isNotEmpty()) {
                                            checklistItems.forEach { item ->
                                                val prefix = if (item.isChecked) "[x]" else "[ ]"
                                                appendLine("$prefix ${item.text}")
                                            }
                                        } else {
                                            appendLine(note.content)
                                        }
                                        appendLine("---")
                                        appendLine("Créé via Notebook Applet")
                                    }
                                    
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Partager la note via")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Partager la note", tint = contrastOnColor)
                            }

                            // Delete note action
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer la note", tint = contrastOnColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = onEdit,
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        text = { Text("Modifier") },
                        containerColor = contrastOnColor,
                        contentColor = noteColor,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("edit_note_fab")
                    )
                }
            ) { contentPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Display Title
                    if (note.title.isNotEmpty()) {
                        item {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = contrastOnColor
                            )
                        }
                    }

                    // Display date / metadata clock
                    item {
                        val formatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy 'à' HH:mm", Locale.getDefault()) }
                        val formattedDate = formatter.format(Date(note.timestamp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Separation line spacer
                    item {
                        Divider(
                            color = contrastOnColor.copy(alpha = 0.15f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Body content - Free text or Interactive list
                    if (checklistItems.isNotEmpty()) {
                        item {
                            Text(
                                "LISTE DES TÂCHES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = subtitleColor.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }

                        items(checklistItems.size) { index ->
                            val item = checklistItems[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(contrastOnColor.copy(alpha = 0.05f))
                                    .clickable {
                                        val nextChecked = !item.isChecked
                                        viewModel.toggleChecklistItem(note, index, nextChecked)
                                        val updatedList = checklistItems.toMutableList()
                                        updatedList[index] = item.copy(isChecked = nextChecked)
                                        checklistItems = updatedList
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Coché/Décoché",
                                        tint = contrastOnColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color.Transparent, CircleShape)
                                            .border(2.dp, contrastOnColor, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (item.isChecked) contrastOnColor.copy(alpha = 0.5f) else contrastOnColor,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = contrastOnColor,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // Bottom item spacer
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}
