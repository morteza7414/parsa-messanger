package com.example.parsamessenger

import android.telephony.SmsManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class Message(
    val id: Long,
    val text: String,
    val mine: Boolean,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    username: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db =
        remember {
            AppDatabase.getDatabase(context)
        }

    val dao =
        remember {
            db.messageDao()
        }

    val key =
        PhoneUtils.normalize(username)

    val displayName =
        ContactNameUtils
            .getName(
                context,
                username
            )

    val avatar = remember {

        ContactsUtils
            .getContactPhoto(
                context,
                username
            )

    }

    val dbMessages by
    dao
        .getMessages(key)
        .collectAsState(
            initial = emptyList()
        )

    val messages =
        dbMessages.map {

            Message(
                it.id,
                it.body,
                it.isMine,
                it.timestamp
            )

        }

    var input by remember {

        mutableStateOf("")

    }

    var editing by remember {

        mutableStateOf<Message?>(null)

    }

    if (editing != null) {

        var edit by remember {

            mutableStateOf(
                editing!!.text
            )

        }

        AlertDialog(

            onDismissRequest = {

                editing = null

            },

            title = {

                Text("Edit Message")

            },

            text = {

                OutlinedTextField(

                    value = edit,

                    onValueChange = {

                        edit = it

                    }

                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        scope.launch(
                            Dispatchers.IO
                        ) {

                            dao.updateMessage(

                                MessageEntity(

                                    id =
                                        editing!!.id,

                                    address =
                                        key,

                                    body =
                                        edit,

                                    isMine =
                                        editing!!.mine,

                                    timestamp =
                                        editing!!.timestamp

                                )

                            )

                        }

                        editing = null

                    }

                ) {

                    Text("Save")

                }

            }

        )

    }

    Column(

        Modifier
            .fillMaxSize()
            .imePadding()

    ) {

        TopAppBar(

            navigationIcon = {

                IconButton(
                    onClick =
                        onBack
                ) {

                    Icon(
                        Icons.Default.ArrowBack,
                        null
                    )

                }

            },

            title = {

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    if (
                        avatar != null
                    ) {

                        AsyncImage(

                            model =
                                avatar,

                            contentDescription =
                                null,

                            modifier =

                                Modifier
                                    .size(
                                        46.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )

                        )

                    }

                    Spacer(
                        Modifier.width(
                            12.dp
                        )
                    )

                    Column {

                        Text(
                            displayName
                        )

                        Text(

                            "Online",

                            color =
                                Color.Gray,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall

                        )

                    }

                }

            },

            actions = {

                IconButton(
                    {}
                ) {

                    Icon(
                        Icons.Default.Call,
                        null
                    )

                }

                IconButton(
                    {}
                ) {

                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = null
                    )

                }

                IconButton(
                    {}
                ) {

                    Icon(
                        Icons.Default.MoreVert,
                        null
                    )

                }

            }

        )

        HorizontalDivider()

        LazyColumn(

            modifier =
                Modifier
                    .weight(1f),

            contentPadding =
                PaddingValues(
                    14.dp
                )

        ) {

            items(
                messages,
                key = {
                    it.id
                }
            ) {

                MessageBubble(

                    message = it,

                    onDelete = {

                        scope.launch(
                            Dispatchers.IO
                        ) {

                            dao.deleteMessage(

                                dbMessages.first {

                                        m ->

                                    m.id ==
                                            it.id

                                }

                            )

                        }

                    },

                    onEdit = {

                        editing =
                            it

                    }

                )

            }

        }

        Surface(

            tonalElevation =
                4.dp

        ) {

            Row(

                modifier =

                    Modifier
                        .padding(
                            10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                OutlinedTextField(

                    value =
                        input,

                    onValueChange = {

                        input = it

                    },

                    modifier =
                        Modifier
                            .weight(1f),

                    placeholder = {

                        Text(
                            "iMessage"
                        )

                    },

                    shape =

                        RoundedCornerShape(
                            30.dp
                        )

                )

                Spacer(
                    Modifier.width(
                        8.dp
                    )
                )

                FloatingActionButton(

                    onClick = {

                        if (
                            input
                                .isBlank()
                        )
                            return@FloatingActionButton

                        val text =
                            input

                        input = ""

                        try {

                            context
                                .getSystemService(
                                    SmsManager::class.java
                                )

                                .sendTextMessage(

                                    key,

                                    null,

                                    text,

                                    null,

                                    null

                                )

                            scope.launch(
                                Dispatchers.IO
                            ) {

                                dao.insert(

                                    MessageEntity(

                                        address =
                                            key,

                                        body =
                                            text,

                                        isMine =
                                            true,

                                        timestamp =
                                            System.currentTimeMillis()

                                    )

                                )

                            }

                        }

                        catch (
                            _: Exception
                        ) {
                        }

                    },

                    containerColor =
                        Color(
                            0xFF007AFF
                        )

                ) {

                    Icon(
                        Icons.Default.Send,
                        null
                    )

                }

            }

        }

    }

}

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun MessageBubble(

    message: Message,

    onDelete: () -> Unit,

    onEdit: () -> Unit

) {

    val offset =
        remember {

            Animatable(0f)

        }

    val scope =
        rememberCoroutineScope()

    val time =

        remember {

            SimpleDateFormat(

                "HH:mm",

                Locale.getDefault()

            )

        }

    Box(

        modifier =

            Modifier

                .fillMaxWidth()

                .offset {

                    IntOffset(

                        offset
                            .value
                            .roundToInt(),

                        0

                    )

                }

                .pointerInput(Unit) {

                    detectHorizontalDragGestures(

                        onHorizontalDrag = {

                                _,
                                dragAmount ->

                            scope.launch {

                                offset.snapTo(
                                    offset.value +
                                            dragAmount
                                )

                            }

                        },

                        onDragEnd = {

                            if (

                                offset.value <
                                -220

                            ) {

                                onDelete()

                            }

                            else {

                                scope.launch {

                                    offset.animateTo(

                                        0f,

                                        tween(200)

                                    )

                                }

                            }

                        }

                    )

                },

        contentAlignment =

            if (
                message.mine
            )

                Alignment.CenterEnd

            else

                Alignment.CenterStart

    ) {

        Card(

            modifier =

                Modifier
                    .combinedClickable(

                        onClick = {},

                        onLongClick = {

                            onEdit()

                        }

                    ),

            colors =

                CardDefaults
                    .cardColors(

                        if (
                            message.mine
                        )

                            Color(
                                0xFF2962FF
                            )

                        else

                            MaterialTheme
                                .colorScheme
                                .surfaceVariant

                    ),

            shape =

                RoundedCornerShape(
                    26.dp
                )

        ) {

            Column(

                Modifier.padding(
                    14.dp
                )

            ) {

                Text(

                    message.text,

                    color =

                        if (
                            message.mine
                        )

                            Color.White

                        else

                            MaterialTheme
                                .colorScheme
                                .onSurface

                )

                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Text(

                    text =

                        time.format(

                            Date(
                                message.timestamp
                            )

                        ) + if (
                            message.mine
                        )

                            " ✓✓"

                        else "",

                    color =
                        Color.Gray

                )

            }

        }

    }

}