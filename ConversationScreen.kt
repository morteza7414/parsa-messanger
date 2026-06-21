package com.example.parsamessenger

import android.app.PendingIntent
import android.content.Intent
import android.telephony.SmsManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var deletingMessage by remember { mutableStateOf<MessageEntity?>(null) }

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

    /* Edit dialog */
    editingMessage?.let { msg ->
        var editValue by remember { mutableStateOf(msg.body) }

        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth()
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

    /* Delete confirmation */
    deletingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            title = { Text("Delete message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        dao.deleteMessage(msg)
                    }
                    deletingMessage = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // جایگزین کنید در بخش Scaffold -> topBar
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clip(RoundedCornerShape(26.dp))
                        // استفاده از رنگ surface با شفافیت، که در لایت و دارک مود خودش رو وفق میده
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // دکمه بازگشت
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .clickable { onBack() }, // اضافه کردن قابلیت کلیک
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // آواتار و متن‌ها
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (avatar != null) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        displayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // دکمه منو
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

        }
    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dbMessages, key = { it.id }) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically { it } + fadeIn()
                    ) {
                        MessageBubble(
                            message = msg,
                            onDelete = { deletingMessage = msg },
                            onEdit = { editingMessage = msg },
                            onResend = { resendMessage(context, dao, it) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(.4f),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        if (input.isBlank()) return@IconButton
                        sendMessage(context, dao, key, input.trim())
                        input = ""
                    }
                ) {
                    Icon(Icons.Default.Send, null, tint = BluePrimary)
                }
            }
        }
    }
}

/* send message */
fun sendMessage(
    context: android.content.Context,
    dao: MessageDao,
    address: String,
    text: String
) {
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    scope.launch {
        val message = MessageEntity(
            address = address,
            body = text,
            isMine = true,
            timestamp = System.currentTimeMillis(),
            sent = false,
            delivered = false,
            isRead = false,
            failed = false
        )

        val messageId = dao.insert(message)

        val smsManager = context.getSystemService(SmsManager::class.java)

        val sentIntent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            Intent(context, SmsSentReceiver::class.java).putExtra("messageId", messageId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            Intent(context, SmsDeliveredReceiver::class.java).putExtra("messageId", messageId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        smsManager.sendTextMessage(
            address,
            null,
            text,
            sentIntent,
            deliveredIntent
        )
    }
}

/* resend */
fun resendMessage(
    context: android.content.Context,
    dao: MessageDao,
    message: MessageEntity
) {
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    scope.launch {
        dao.updateMessage(
            message.copy(
                failed = false,
                sent = false,
                delivered = false
            )
        )

        val smsManager = context.getSystemService(SmsManager::class.java)

        val sentIntent = PendingIntent.getBroadcast(
            context,
            message.id.toInt(),
            Intent(context, SmsSentReceiver::class.java).putExtra("messageId", message.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            message.id.toInt(),
            Intent(context, SmsDeliveredReceiver::class.java).putExtra("messageId", message.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        smsManager.sendTextMessage(
            message.address,
            null,
            message.body,
            sentIntent,
            deliveredIntent
        )
    }
}

/* bubble */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onResend: (MessageEntity) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val time = formatTime(message.timestamp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, drag ->
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag)
                        }
                    },
                    onDragEnd = {
                        if (offsetX.value < -220f) {
                            onDelete()
                        }
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
            },
        contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onEdit
                ),
                shape = RoundedCornerShape(20.dp),
                color = if (message.isMine)
                    BluePrimary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    message.body,
                    modifier = Modifier.padding(14.dp),
                    color = if (message.isMine) Color.White
                    else MaterialTheme.colorScheme.onSurface
                )
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

                    when {
                        message.failed -> {
                            Text(
                                "Resend",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    onResend(message)
                                }
                            )
                        }

                        message.delivered -> {
                            Icon(
                                Icons.Default.DoneAll,
                                null,
                                tint = Color(0xFF1DA1F2),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        message.sent -> {
                            Icon(
                                Icons.Default.Done,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* smart time */
fun formatTime(timestamp: Long): String {
    val messageDate = Date(timestamp)
    val now = Date()

    val diff = now.time - messageDate.time
    val oneDay = 24 * 60 * 60 * 1000

    return if (diff < oneDay) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
    } else {
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(messageDate)
    }
}
