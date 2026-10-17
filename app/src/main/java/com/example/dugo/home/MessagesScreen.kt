package com.example.dugo.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.login.Brand

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val BubbleMe = Color(0xFFEE1D2B)
private val BubbleThem = Color(0xFFF3F4F6)

// Current user's ID (0 = "me" in sample data)
private const val MY_ID = 0

@Composable
fun MessagesScreen(
    selectedTab: HomeTab = HomeTab.Messages,
    onTabSelected: (HomeTab) -> Unit = {},
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val conversations = remember { mutableStateListOf(*sampleConversations.toTypedArray()) }
    var openConversation by remember { mutableStateOf<Conversation?>(null) }

    val current = openConversation

    if (current != null) {
        ChatScreen(
            conversation = current,
            onBack = { openConversation = null }
        )
    } else {
        ConversationListScreen(
            conversations = conversations,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNotifications = onNotifications,
            onProfile = onProfile,
            onOpen = { conv ->
                // Mark all as read when opening
                val idx = conversations.indexOf(conv)
                if (idx >= 0) {
                    val updated = conv.copy(
                        messages = conv.messages.map { it.copy(isRead = true) }.toMutableList()
                    )
                    conversations[idx] = updated
                    openConversation = updated
                }
            }
        )
    }
}

@Composable
private fun ConversationListScreen(
    conversations: MutableList<Conversation>,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (Conversation) -> Unit
) {
    val totalUnread = conversations.sumOf { it.unreadCount }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Messages", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (totalUnread > 0) "$totalUnread unread" else "All caught up",
                    color = if (totalUnread > 0) BubbleMe else Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
                    .clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Gray)
            }
        }

        HorizontalDivider(color = Line)

        if (conversations.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No messages yet", color = Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Contact a donor to start a conversation.", color = Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(
                    conversations.sortedByDescending { it.lastMessage?.sentAt ?: 0L }
                ) { conv ->
                    ConversationRow(conv, onClick = { onOpen(conv) })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 80.dp))
                }
            }
        }

        HorizontalDivider(color = Line)
        MessagesBottomNav(selectedTab, onTabSelected)
    }
}

@Composable
private fun ConversationRow(conv: Conversation, onClick: () -> Unit) {
    val hasUnread = conv.unreadCount > 0
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFEEF0F3)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Gray, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conv.contact.name,
                    fontSize = 15.sp,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    conv.lastMessage?.timeAgo() ?: "",
                    fontSize = 11.sp,
                    color = if (hasUnread) BubbleMe else Gray
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Blood type chip
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brand.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(conv.contact.bloodType, color = Brand, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    conv.lastMessage?.text ?: "",
                    fontSize = 13.sp,
                    color = if (hasUnread) Ink else Gray,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(BubbleMe),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            conv.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(conversation: Conversation, onBack: () -> Unit) {
    val messages = remember { mutableStateListOf(*conversation.messages.toTypedArray()) }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var nextId by remember { mutableStateOf(messages.size + 100) }

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Chat header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEEF0F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Gray, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(conversation.contact.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(7.dp).clip(CircleShape)
                            .background(if (conversation.contact.availableToday) Color(0xFF2E9E6B) else Gray)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (conversation.contact.availableToday) "Available today" else "Not available",
                        color = if (conversation.contact.availableToday) Color(0xFF2E9E6B) else Gray,
                        fontSize = 12.sp
                    )
                    Text("  •  ${conversation.contact.bloodType}", color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Line)

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        HorizontalDivider(color = Line)

        // Input bar
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…", color = Gray, fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (draft.isNotBlank()) {
                        messages.add(
                            Message(
                                id = nextId++,
                                senderId = MY_ID,
                                receiverId = conversation.contact.id,
                                text = draft.trim()
                            )
                        )
                        conversation.messages.add(messages.last())
                        draft = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7F8FA),
                    unfocusedContainerColor = Color(0xFFF7F8FA),
                    focusedBorderColor = Brand,
                    unfocusedBorderColor = Line
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (draft.isNotBlank()) BubbleMe else Line)
                    .clickable(enabled = draft.isNotBlank()) {
                        if (draft.isNotBlank()) {
                            messages.add(
                                Message(
                                    id = nextId++,
                                    senderId = MY_ID,
                                    receiverId = conversation.contact.id,
                                    text = draft.trim()
                                )
                            )
                            conversation.messages.add(messages.last())
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val isMe = msg.senderId == MY_ID
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .background(if (isMe) BubbleMe else BubbleThem)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    msg.text,
                    color = if (isMe) Color.White else Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    msg.formattedTime(),
                    color = if (isMe) Color.White.copy(alpha = 0.7f) else Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MessagesBottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (active) Brand else Gray)
                Text(
                    tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Brand else Gray
                )
            }
        }
    }
}
