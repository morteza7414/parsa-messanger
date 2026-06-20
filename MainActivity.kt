package com.example.parsamessenger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.role.RoleManager
import android.provider.Telephony

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import coil.compose.AsyncImage

import com.example.parsamessenger.ui.theme.ParsaMessengerTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

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

            )

            !=

            PackageManager.PERMISSION_GRANTED

        ) {

            permissionLauncher.launch(

                Manifest.permission.SEND_SMS

            )

        }

        if (

            Build.VERSION.SDK_INT >=

            Build.VERSION_CODES.Q

        ) {

            val roleManager =

                getSystemService(

                    RoleManager::class.java

                )

            if (

                !roleManager.isRoleHeld(

                    RoleManager.ROLE_SMS

                )

            ) {

                startActivity(

                    roleManager
                        .createRequestRoleIntent(

                            RoleManager.ROLE_SMS

                        )

                )

            }

        }

    }

}

data class Chat(

    val name:String,

    val message:String

)

@Composable
fun MessengerScreen() {

    val context =

        LocalContext.current

    var selectedChat by remember {

        mutableStateOf<Chat?>(null)

    }

    var search by remember {

        mutableStateOf("")

    }

    var showDialog by remember {

        mutableStateOf(false)

    }

    val dao = remember {

        AppDatabase
            .getDatabase(
                context
            )

            .messageDao()

    }

    val roomChats by

    dao

        .getChats()

        .collectAsState(

            initial =

                emptyList()

        )

    val chats =

        roomChats

            .distinctBy {

                PhoneUtils.normalize(

                    it.address

                )

            }

            .map {

                Chat(

                    it.address,

                    it.body

                )

            }

    if (

        selectedChat != null

    ) {

        ConversationScreen(

            username =

                selectedChat!!.name,

            onBack = {

                selectedChat = null

            }

        )

        return

    }

    Scaffold(

        containerColor =

            MaterialTheme
                .colorScheme
                .background,

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    showDialog = true

                }

            ) {

                Text(

                    "+",

                    fontSize = 26.sp

                )

            }

        }

    ) { padding ->

        Column(

            modifier =

                Modifier

                    .fillMaxSize()

                    .padding(

                        padding

                    )

                    .padding(

                        20.dp

                    )

        ) {

            Row(

                modifier =

                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =

                    Arrangement
                        .SpaceBetween,

                verticalAlignment =

                    Alignment.CenterVertically

            ) {

                Text(

                    "Messages",

                    fontSize = 42.sp,

                    fontWeight =

                        FontWeight.ExtraBold

                )

                Row {

                    IconButton({}) {

                        Icon(

                            Icons.Default.Search,

                            null

                        )

                    }

                    IconButton({}) {

                        Icon(

                            Icons.Default.MoreVert,

                            null

                        )

                    }

                }

            }

            Spacer(

                Modifier.height(

                    18.dp

                )

            )

            SearchBar(

                value = search,

                onValueChange = {

                    search = it

                }

            )

            Spacer(

                Modifier.height(

                    14.dp

                )

            )

            LazyColumn(

                verticalArrangement =

                    Arrangement.spacedBy(

                        12.dp

                    )

            ) {

                items(

                    chats.filter {

                        ContactNameUtils

                            .getName(

                                context,

                                it.name

                            )

                            .contains(

                                search,

                                true

                            )

                    }

                ) {

                    ChatItem(

                        it

                    ) {

                        selectedChat = it

                    }

                }

            }

        }

    }

}

@Composable
fun SearchBar(

    value:String,

    onValueChange:(String)->Unit

){

    OutlinedTextField(

        value,

        onValueChange,

        modifier=

            Modifier

                .fillMaxWidth(),

        leadingIcon={

            Icon(

                Icons.Default.Search,

                null

            )

        },

        placeholder={

            Text(

                "Search messages"

            )

        },

        shape=

            RoundedCornerShape(

                28.dp

            )

    )

}

@Composable
fun ChatItem(

    chat:Chat,

    onClick:()->Unit

){

    val context=

        LocalContext.current

    val name=

        ContactNameUtils

            .getName(

                context,

                chat.name

            )

    val photo=

        remember{

            ContactsUtils

                .getContactPhoto(

                    context,

                    chat.name

                )

        }

    Card(

        modifier=

            Modifier

                .fillMaxWidth()

                .clickable{

                    onClick()

                },

        shape=

            RoundedCornerShape(

                28.dp

            ),

        colors=

            CardDefaults.cardColors(

                containerColor=

                    MaterialTheme

                        .colorScheme

                        .surface

            )

    ){

        Row(

            Modifier

                .padding(

                    18.dp

                ),

            verticalAlignment=

                Alignment.CenterVertically

        ){

            if(photo!=null){

                AsyncImage(

                    photo,

                    null,

                    Modifier

                        .size(

                            64.dp

                        )

                        .clip(

                            CircleShape

                        )

                )

            }

            else{

                Box(

                    Modifier

                        .size(

                            64.dp

                        )

                        .clip(

                            CircleShape

                        )

                        .background(

                            MaterialTheme

                                .colorScheme

                                .primary

                        ),

                    contentAlignment=

                        Alignment.Center

                ){

                    Text(

                        name.first()

                            .toString(),

                        color=

                            Color.White

                    )

                }

            }

            Spacer(

                Modifier.width(

                    16.dp

                )

            )

            Column(

                Modifier.weight(

                    1f

                )

            ){

                Text(

                    name,

                    fontWeight=

                        FontWeight.ExtraBold,

                    fontSize=

                        20.sp

                )

                Spacer(

                    Modifier.height(

                        6.dp

                    )

                )

                Text(

                    chat.message,

                    maxLines=2,

                    overflow=

                        TextOverflow.Ellipsis,

                    color=

                        MaterialTheme

                            .colorScheme

                            .onSurfaceVariant

                )

            }

            Text(

                "12:30",

                color=

                    Color.Gray

            )

        }

    }

}

