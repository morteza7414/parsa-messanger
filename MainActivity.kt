package com.example.parsamessenger

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.parsamessenger.ui.theme.BluePrimary
import com.example.parsamessenger.ui.theme.ParsaMessengerTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt




class MainActivity : ComponentActivity() {

    private var hasStartedSetup = false

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val readSmsGranted = result[Manifest.permission.READ_SMS] == true
        if (readSmsGranted) {
            lifecycleScope.launch {
                ImportManager.runIfNeeded(this@MainActivity)
            }
        }

        requestDefaultSmsRoleIfNeeded()
    }

    private val defaultSmsRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ParsaMessengerTheme {
                MessengerScreen()
            }
        }

        if (!hasStartedSetup) {
            hasStartedSetup = true
            startInitialSetup()
        }
    }

    private fun startInitialSetup() {
        val permissionsToRequest = buildList {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (!hasPermission(Manifest.permission.RECEIVE_SMS)) {
                add(Manifest.permission.RECEIVE_SMS)
            }
            if (!hasPermission(Manifest.permission.READ_SMS)) {
                add(Manifest.permission.READ_SMS)
            }
            if (!hasPermission(Manifest.permission.SEND_SMS)) {
                add(Manifest.permission.SEND_SMS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            lifecycleScope.launch {
                ImportManager.runIfNeeded(this@MainActivity)
            }
            requestDefaultSmsRoleIfNeeded()
        }
    }

    private fun requestDefaultSmsRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)

            if (
                roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                defaultSmsRoleLauncher.launch(intent)
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }
                defaultSmsRoleLauncher.launch(intent)
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

data class Chat(
    val name: String,
    val message: String,
    val isUnread: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen() {
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chatToAddToCategory by remember { mutableStateOf<Chat?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var search by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var selectedBottomItem by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val mutedChats = remember {
        mutableStateListOf<String>().apply {
            addAll(loadMutedChats(context))
        }
    }

    val categoryDao = remember { AppDatabase.getDatabase(context).categoryDao() }
    val categoriesFromDb by categoryDao.getCategories().collectAsState(initial = emptyList())
    val selectedCategoryEntity =
        categoriesFromDb.find { it.title == selectedCategory }

    val addressesInSelectedCategory by
    (selectedCategoryEntity?.let {
        categoryDao.getChatsInCategory(it.id)
    } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())


    val categories = listOf("All", "Personal") + categoriesFromDb.map { it.title }


    val dao = remember { AppDatabase.getDatabase(context).messageDao() }
    val roomChats by dao.getConversations().collectAsState(initial = emptyList())

    BackHandler(enabled = selectedChat != null) {
        selectedChat = null
    }

    val chats = roomChats
        .distinctBy { PhoneUtils.normalize(it.address) }
        .map {
            Chat(
                name = it.address,
                message = it.body,
                isUnread = it.hasUnread
            )
        }

    val filteredChats = chats.filter { chat ->

        val normalizedAddress = PhoneUtils.normalize(chat.name)
        val displayName = ContactNameUtils.getName(context, chat.name)

        // ---------- SEARCH ----------
        val matchesSearch =
            if (showSearch && search.isNotBlank()) {
                displayName.contains(search, ignoreCase = true) ||
                        chat.name.contains(search, ignoreCase = true) ||
                        chat.message.contains(search, ignoreCase = true)
            } else {
                true
            }

        // ---------- CATEGORY ----------
        val matchesCategory =
            when (selectedCategory) {

                "All" -> true

                "Personal" ->
                    !isBankLikeMessage(chat.name, chat.message)

                "Bank" ->
                    isBankLikeMessage(chat.name, chat.message)

                else -> {
                    // ✅ فقط اگر داخل CrossRef ذخیره شده باشد
                    addressesInSelectedCategory.contains(normalizedAddress)
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
                    mutedChats = mutedChats,
                    search = search,
                    showSearch = showSearch,
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onSearchChange = { search = it },
                    onToggleSearch = {
                        showSearch = !showSearch
                        if (!showSearch) search = ""
                    },
                    onCategorySelected = { selectedCategory = it },
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onChatClick = { chat -> selectedChat = chat },
                    onChatDeleteRequest = { chat ->
                        chatToDelete = chat
                    },
                    onChatMuteRequest = { chat ->
                        val normalized = PhoneUtils.normalize(chat.name)

                        if (mutedChats.contains(normalized)) {
                            mutedChats.remove(normalized)
                        } else {
                            mutedChats.add(normalized)
                        }

                        saveMutedChats(context, mutedChats.toSet())
                    },
                    onCategoryLongClick = { category ->
                        if (category != "All" && category != "Personal" && category != "Bank") {
                            categoryToDelete = category
                        }
                    },
                    onAddToCategory = { chat ->
                        chatToAddToCategory = chat
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
                    scope.launch {
                        categoryDao.insertCategory(CategoryEntity(title = cleanTitle))
                    }

                    selectedCategory = cleanTitle
                }
                showAddCategoryDialog = false
            }
        )
    }

    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chat = chatToDelete

                        if (chat != null) {
                            scope.launch {
                                dao.deleteConversation(chat.name)
                            }
                        }

                        chatToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        chatToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete conversation?") },
            text = { Text("This chat will be permanently deleted.") }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Do you want to delete \"$category\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
//                        categories.remove(category)
                        selectedCategory = "All"
                        categoryToDelete = null
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("No")
                }
            }
        )
    }

    chatToAddToCategory?.let { chat ->

        val chatAddress = PhoneUtils.normalize(chat.name)

        val chatCategories by categoryDao
            .getCategoriesForChat(chatAddress)
            .collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { chatToAddToCategory = null },
            title = { Text("Categories") },

            text = {
                Column {

                    categoriesFromDb.forEach { category ->

                        val checked = chatCategories.contains(category.id)

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->

                                    scope.launch {

                                        if (isChecked) {

                                            categoryDao.addChatToCategory(
                                                ChatCategoryCrossRef(
                                                    address = chatAddress,
                                                    categoryId = category.id
                                                )
                                            )

                                        } else {

                                            categoryDao.removeChatFromCategory(
                                                chatAddress,
                                                category.id
                                            )
                                        }
                                    }
                                }
                            )

                            Text(category.title)
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = { chatToAddToCategory = null }
                ) {
                    Text("Done")
                }
            }
        )
    }




}

@Composable
private fun MessagesHomeContent(
    modifier: Modifier = Modifier,
    chats: List<Chat>,
    mutedChats: List<String>,
    search: String,
    showSearch: Boolean,
    selectedCategory: String,
    categories: List<String>,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onChatClick: (Chat) -> Unit,
    onChatDeleteRequest: (Chat) -> Unit,
    onChatMuteRequest: (Chat) -> Unit,
    onCategoryLongClick: (String) -> Unit,
    onAddToCategory: (Chat) -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp)
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
            onAddCategoryClick = onAddCategoryClick,
            onCategoryLongClick = onCategoryLongClick
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
                    ChatItem(
                        chat = chat,
                        isMuted = mutedChats.contains(PhoneUtils.normalize(chat.name)),
                        onClick = { onChatClick(chat) },
                        onDeleteRequest = { onChatDeleteRequest(it) },
                        onMuteRequest = { onChatMuteRequest(it) },
                        onLongClick = { onAddToCategory(chat) }
                    )
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

        Row(verticalAlignment = Alignment.CenterVertically) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryLongClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { title ->
            val isSelected = selectedCategory == title

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            BluePrimary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        }
                    )
                    .combinedClickable(
                        onClick = { onCategorySelected(title) },
                        onLongClick = { onCategoryLongClick(title) }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    chat: Chat,
    isMuted: Boolean,
    onClick: () -> Unit,
    onDeleteRequest: (Chat) -> Unit,
    onMuteRequest: (Chat) -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val name = ContactNameUtils.getName(context, chat.name)
    val photo = remember(chat.name) { ContactsUtils.getContactPhoto(context, chat.name) }

    val offsetX = remember { Animatable(0f) }
    var itemWidthPx by remember { mutableStateOf(0f) }

    val actionWidthPx = with(density) { 150.dp.toPx() }
    val revealThresholdPx = with(density) { 56.dp.toPx() }

    val deleteThresholdPx =
        if (itemWidthPx > 0f) {
            itemWidthPx * 0.70f
        } else {
            with(density) { 180.dp.toPx() }
        }

    val swipeProgress = (-offsetX.value / actionWidthPx).coerceIn(0f, 1f)

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = if (chat.isUnread) 0.96f else 0.88f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (chat.isUnread) 0.54f else 0.42f),
            MaterialTheme.colorScheme.surface.copy(alpha = if (chat.isUnread) 0.78f else 0.70f)
        )
    )




    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                itemWidthPx = size.width.toFloat()
            }
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .alpha(swipeProgress)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeActionButton(
                title = if (isMuted) "Muted" else "Mute",
                color = Color(0xFF8E8E93),
                icon = Icons.Default.NotificationsOff,
                onClick = {
                    scope.launch {
                        onMuteRequest(chat)
                        offsetX.animateTo(0f, tween(220))
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            SwipeActionButton(
                title = "Delete",
                color = Color(0xFFFF3B30),
                icon = Icons.Default.Delete,
                onClick = {
                    scope.launch {
                        offsetX.animateTo(0f, tween(160))
                        onDeleteRequest(chat)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(offsetX.value.roundToInt(), 0)
                }
                .pointerInput(chat.name) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val nextOffset = (offsetX.value + dragAmount)
                                .coerceIn(-itemWidthPx, 0f)

                            scope.launch {
                                offsetX.snapTo(nextOffset)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val currentOffset = -offsetX.value

                                when {
                                    currentOffset >= deleteThresholdPx -> {
                                        offsetX.animateTo(0f, tween(180))
                                        onDeleteRequest(chat)
                                    }

                                    currentOffset >= revealThresholdPx -> {
                                        offsetX.animateTo(-actionWidthPx, tween(240))
                                    }

                                    else -> {
                                        offsetX.animateTo(0f, tween(220))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, tween(220))
                            }
                        }
                    )
                }
                .clip(RoundedCornerShape(32.dp))
                .background(glassBrush)
                .border(
                    width = if (chat.isUnread) 1.4.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (chat.isUnread) 0.62f else 0.45f),
                            MaterialTheme.colorScheme.outline.copy(alpha = if (chat.isUnread) 0.22f else 0.14f),
                            Color.White.copy(alpha = if (chat.isUnread) 0.22f else 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )

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
                                        BluePrimary.copy(alpha = if (chat.isUnread) 0.30f else 0.20f),
                                        BluePrimary.copy(alpha = if (chat.isUnread) 0.14f else 0.08f)
                                    )
                                )
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontWeight = if (chat.isUnread) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isMuted) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = chat.message,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (chat.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (chat.isUnread) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.96f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
                        }
                    )
                }
            }

            if (chat.isUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 48.dp, bottom = 10.dp)
                ) {
                    AnimatedUnreadOrb()
                }
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.92f))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(23.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AnimatedUnreadOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "UnreadOrbTransition")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "UnreadOrbRotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "UnreadOrbPulse"
    )

    Canvas(
        modifier = Modifier
            .size((13 * pulse).dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = BluePrimary.copy(alpha = 0.35f),
                spotColor = Color(0xFF9C27B0).copy(alpha = 0.35f)
            )
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF2979FF),
                    Color(0xFF9C27B0),
                    Color(0xFFFF2D95),
                    Color(0xFF00E5FF)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            alpha = 0.96f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color.White.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x + kotlin.math.cos(Math.toRadians(rotation.toDouble())).toFloat() * radius * 0.28f,
                    y = center.y + kotlin.math.sin(Math.toRadians(rotation.toDouble())).toFloat() * radius * 0.28f
                ),
                radius = radius * 1.1f,
                tileMode = TileMode.Clamp
            ),
            radius = radius,
            center = center
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = radius * 0.22f,
            center = Offset(
                x = center.x - radius * 0.32f,
                y = center.y - radius * 0.36f
            )
        )
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
        modifier = modifier.padding(horizontal = 20.dp)
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
            TextButton(onClick = { onAdd(title) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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




private fun loadMutedChats(context: Context): Set<String> {
    return context
        .getSharedPreferences("chat_settings", Context.MODE_PRIVATE)
        .getStringSet("muted_chats", emptySet())
        ?: emptySet()
}

private fun saveMutedChats(
    context: Context,
    mutedChats: Set<String>
) {
    context
        .getSharedPreferences("chat_settings", Context.MODE_PRIVATE)
        .edit()
        .putStringSet("muted_chats", mutedChats)
        .apply()
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
