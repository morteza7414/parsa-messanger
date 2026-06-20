package com.example.parsamessenger

import android.telephony.SmsManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.parsamessenger.ui.theme.BluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    username: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).messageDao() }

    val key = PhoneUtils.normalize(username)

    val displayName = ContactNameUtils.getName(context, username)
    val avatar = remember { ContactsUtils.getContactPhoto(context, username) }

    val dbMessages by dao.getMessages(key).collectAsState(initial = emptyList())

    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }

    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }

    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(dbMessages.size) {
        if (dbMessages.isNotEmpty()) {
            listState.animateScrollToItem(dbMessages.size - 1)
        }
    }

    LaunchedEffect(Unit) {

        scope.launch(Dispatchers.IO) {
            dao.markAsRead(key)
        }
    }


    editingMessage?.let { msg ->

        var editValue by remember { mutableStateOf(msg.body) }

        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = {

                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

            },
            confirmButton = {

                TextButton(onClick = {

                    scope.launch(Dispatchers.IO) {
                        dao.updateMessage(msg.copy(body = editValue))
                    }

                    editingMessage = null

                }) { Text("Save") }

            }
        )
    }

    Scaffold(

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }

                },

                title = {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        if (avatar != null) {

                            AsyncImage(
                                model = avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )

                        } else {

                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(displayName.take(1), color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {

                            Text(
                                displayName,
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            )

                            Text(
                                "Online",
                                style = TextStyle(fontSize = 12.sp, color = Color(0xFF4CAF50))
                            )
                        }
                    }
                },

                actions = {

                    IconButton({}) { Icon(Icons.Default.Call, null) }

                    IconButton({}) { Icon(Icons.Default.Videocam, null) }

                    IconButton({}) { Icon(Icons.Default.MoreVert, null) }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                )
            )
        },

        containerColor = MaterialTheme.colorScheme.background

    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(dbMessages, key = { it.id }) { msg ->

                    MessageBubble(
                        message = msg,
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                dao.deleteMessage(msg)
                            }
                        },
                        onEdit = { editingMessage = msg }
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = Color.Transparent
            ) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)

                ) {

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, null, tint = Color.Gray)
                    }

                    TextField(

                        value = input,

                        onValueChange = { input = it },

                        modifier = Modifier.weight(1f),

                        placeholder = {
                            Text("Type a message...", color = Color.Gray)
                        },

                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),

                        colors = TextFieldDefaults.colors(

                            focusedContainerColor = Color.Transparent,

                            unfocusedContainerColor = Color.Transparent,

                            focusedIndicatorColor = Color.Transparent,

                            unfocusedIndicatorColor = Color.Transparent,

                            cursorColor = BluePrimary
                        )
                    )

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Face, null, tint = Color.Gray)
                    }

                    FloatingActionButton(

                        onClick = {

                            if (input.isBlank() || isSending) return@FloatingActionButton

                            val text = input.trim()

                            input = ""

                            isSending = true

                            try {

                                val smsManager = context.getSystemService(SmsManager::class.java)

                                smsManager.sendTextMessage(key, null, text, null, null)

                                scope.launch(Dispatchers.IO) {

                                    dao.insert(
                                        MessageEntity(
                                            address = key,
                                            body = text,
                                            isMine = true,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            isSending = false
                        },

                        modifier = Modifier.size(48.dp),

                        containerColor = BluePrimary,

                        shape = CircleShape,

                        elevation = FloatingActionButtonDefaults.elevation(0.dp)

                    ) {

                        Icon(
                            if (input.isEmpty()) Icons.Default.Mic else Icons.Default.Send,
                            null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    val offset = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    val time = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .pointerInput(Unit) {

                detectHorizontalDragGestures(

                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch { offset.snapTo(offset.value + dragAmount) }
                    },

                    onDragEnd = {

                        if (offset.value < -250f) {

                            onDelete()

                        } else {

                            scope.launch {
                                offset.animateTo(0f, tween(200))
                            }
                        }
                    }
                )
            },

        contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {

        val gradient = Brush.horizontalGradient(
            listOf(BluePrimary, Color(0xFF00C6FF))
        )

        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {

            Surface(

                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onEdit
                ),

                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (message.isMine) 22.dp else 4.dp,
                    bottomEnd = if (message.isMine) 4.dp else 22.dp
                ),

                color = if (message.isMine)
                    Color.Transparent
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

            ) {

                Box(
                    modifier = if (message.isMine)
                        Modifier
                            .background(gradient)
                            .padding(14.dp)
                    else
                        Modifier.padding(14.dp)
                ) {

                    Text(
                        message.body,
                        color = if (message.isMine)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {

                Text(
                    time,
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                if (message.isMine) {

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        Icons.Default.DoneAll,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = BluePrimary
                    )
                }
            }
        }
    }
}
