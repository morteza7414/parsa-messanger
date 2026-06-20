package com.example.parsamessenger

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.parsamessenger.ui.theme.BluePrimary
import com.example.parsamessenger.ui.theme.ParsaMessengerTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.content.Intent
import android.provider.Telephony


class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = AppDatabase.getDatabase(this).messageDao()

        lifecycleScope.launch {
            loadExistingSms(this@MainActivity, dao)
        }

        requestDefaultSmsApp(this)



        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
            ),
            100
        )

        requestSmsRole()
        enableEdgeToEdge()



        setContent {
            ParsaMessengerTheme {
                MessengerScreen()
            }
        }
    }

    private fun requestSmsRole() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.SEND_SMS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            }
        }
    }
}

data class Chat(
    val name: String,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen() {
    val context = LocalContext.current

    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var search by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var selectedBottomItem by remember { mutableStateOf(0) }

    var selectedCategory by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val categories = remember {
        mutableStateListOf(
            "All",
            "Personal",
            "Bank"
        )
    }

    val dao = remember { AppDatabase.getDatabase(context).messageDao() }
    val roomChats by dao.getConversations().collectAsState(initial = emptyList())


    val chats = roomChats
        .distinctBy { PhoneUtils.normalize(it.address) }
        .map { Chat(it.address, it.body) }

    val filteredChats = chats
        .filter { chat ->
            val displayName = ContactNameUtils.getName(context, chat.name)

            val matchesSearch =
                if (showSearch && search.isNotBlank()) {
                    displayName.contains(search, ignoreCase = true) ||
                            chat.name.contains(search, ignoreCase = true) ||
                            chat.message.contains(search, ignoreCase = true)
                } else {
                    true
                }

            val matchesCategory =
                when (selectedCategory) {
                    "All" -> true

                    "Personal" -> {
                        !isBankLikeMessage(chat.name, chat.message)
                    }

                    "Bank" -> {
                        isBankLikeMessage(chat.name, chat.message)
                    }

                    else -> {
                        displayName.contains(selectedCategory, ignoreCase = true) ||
                                chat.name.contains(selectedCategory, ignoreCase = true) ||
                                chat.message.contains(selectedCategory, ignoreCase = true)
                    }
                }

            matchesSearch && matchesCategory
        }

    if (selectedChat != null) {
        ConversationScreen(
            username = selectedChat!!.name,
            onBack = { selectedChat = null }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomItem == 0,
                    onClick = { selectedBottomItem = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Messages"
                        )
                    },
                    label = { Text("Messages") }
                )

                NavigationBarItem(
                    selected = selectedBottomItem == 1,
                    onClick = { selectedBottomItem = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Contacts"
                        )
                    },
                    label = { Text("Contacts") }
                )
            }
        },
        floatingActionButton = {
            if (selectedBottomItem == 0) {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = BluePrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Message",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->

        when (selectedBottomItem) {
            0 -> {
                MessagesHomeContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    chats = filteredChats,
                    search = search,
                    showSearch = showSearch,
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onSearchChange = { search = it },
                    onToggleSearch = {
                        showSearch = !showSearch
                        if (!showSearch) {
                            search = ""
                        }
                    },
                    onCategorySelected = { selectedCategory = it },
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onChatClick = { chat ->
                        selectedChat = chat
                    }
                )
            }

            1 -> {
                ContactsPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onContactClick = { number ->
                        selectedChat = Chat(number, "")
                    }
                )
            }
        }
    }

    if (showNewChatSheet) {
        NewChatSheet(
            onDismiss = { showNewChatSheet = false },
            onContactSelected = { number ->
                selectedChat = Chat(number, "")
                showNewChatSheet = false
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { title ->
                val cleanTitle = title.trim()
                if (
                    cleanTitle.isNotBlank() &&
                    categories.none { it.equals(cleanTitle, ignoreCase = true) }
                ) {
                    categories.add(cleanTitle)
                    selectedCategory = cleanTitle
                }
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun MessagesHomeContent(
    modifier: Modifier = Modifier,
    chats: List<Chat>,
    search: String,
    showSearch: Boolean,
    selectedCategory: String,
    categories: List<String>,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onChatClick: (Chat) -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        MessagesHeader(
            showSearch = showSearch,
            onToggleSearch = onToggleSearch
        )

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(
                    value = search,
                    onValueChange = onSearchChange
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            onAddCategoryClick = onAddCategoryClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (chats.isEmpty()) {
            EmptyChatsState(
                selectedCategory = selectedCategory,
                showSearch = showSearch,
                search = search
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = chats,
                    key = { chat -> PhoneUtils.normalize(chat.name) }
                ) { chat ->
                    ChatItem(chat = chat) {
                        onChatClick(chat)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesHeader(
    showSearch: Boolean,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Messages",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (showSearch) {
                            BluePrimary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    )
            ) {
                Icon(
                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (showSearch) "Close Search" else "Search",
                    tint = if (showSearch) BluePrimary else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search",
                        tint = Color.Gray
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        },
        placeholder = {
            Text(
                text = "Search messages",
                color = Color.Gray
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = BluePrimary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { title ->
            FilterChip(
                selected = selectedCategory == title,
                onClick = { onCategorySelected(title) },
                label = { Text(title) },
                shape = RoundedCornerShape(20.dp),
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BluePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
        }

        OutlinedButton(
            onClick = onAddCategoryClick,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                color = BluePrimary.copy(alpha = 0.45f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Category",
                modifier = Modifier.size(17.dp),
                tint = BluePrimary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Add",
                color = BluePrimary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val name = ContactNameUtils.getName(context, chat.name)
    val photo = remember(chat.name) { ContactsUtils.getContactPhoto(context, chat.name) }

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(glassBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BluePrimary.copy(alpha = 0.20f),
                                    BluePrimary.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = BluePrimary.copy(alpha = 0.18f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "12:30 PM",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = chat.message,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun ContactsPage(
    modifier: Modifier = Modifier,
    onContactClick: (String) -> Unit
) {
    val context = LocalContext.current
    val contacts = remember { ContactsUtils.getContacts(context) }

    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Contacts",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(contacts) { contact ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            text = contact.number,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1).uppercase(),
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onContactClick(contact.number) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyChatsState(
    selectedCategory: String,
    showSearch: Boolean,
    search: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (showSearch && search.isNotBlank()) {
                    "No results found"
                } else {
                    "No messages in $selectedCategory"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (showSearch && search.isNotBlank()) {
                    "Try searching with another name, number or message."
                } else {
                    "Your conversations will appear here."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Category",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                placeholder = { Text("Example: Bank, Family, Work") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = BluePrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title) }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatSheet(
    onDismiss: () -> Unit,
    onContactSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    val contacts = remember { ContactsUtils.getContacts(context) }

    val filtered = contacts.filter {
        it.name.contains(text, ignoreCase = true) || it.number.contains(text)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "New Message",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "To: ",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = BluePrimary
                    ),
                    placeholder = {
                        Text(
                            text = "Name or phone number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    },
                    singleLine = true
                )

                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = { onContactSelected(text.trim()) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(BluePrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Start Chat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(filtered) { contact ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = contact.number,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onContactSelected(contact.number) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

private fun isBankLikeMessage(
    sender: String,
    body: String
): Boolean {
    val text = "$sender $body".lowercase()

    val bankKeywords = listOf(
        "bank",
        "بانک",
        "ملت",
        "ملی",
        "صادرات",
        "تجارت",
        "سامان",
        "پاسارگاد",
        "پارسیان",
        "آینده",
        "رسالت",
        "سپه",
        "رفاه",
        "کشاورزی",
        "انصار",
        "دی",
        "گردشگری",
        "سرمایه",
        "شهر",
        "کارآفرین",
        "خاورمیانه",
        "اقتصادنوین",
        "واریز",
        "برداشت",
        "مانده",
        "موجودی",
        "رمز",
        "otp",
        "کارت",
        "حساب",
        "تراکنش",
        "خرید",
        "پرداخت",
        "pos",
        "atm",
        "شبا"
    )

    return bankKeywords.any { keyword ->
        text.contains(keyword)
    }
}


fun requestDefaultSmsApp(activity: Activity) {

    if (Telephony.Sms.getDefaultSmsPackage(activity) != activity.packageName) {

        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)

        intent.putExtra(
            Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
            activity.packageName
        )

        activity.startActivity(intent)
    }
}
