# DuGO – Messages Page UI Only

This file has **only** the Messages page UI (the "Your messages" screen from your design). It has no notifications, no Requests page, no Facebook button and no algorithm.

Every block starts with a comment banner that says:
- **FILE** – which file to open or create
- **ACTION** – create new / replace / add under a line
- **WHERE** – the exact spot

Paths are relative to `app/src/main/java/com/example/dugo/` unless it says otherwise.

---

## STEP 1 – Create `home/MessagesScreen.kt`

By default the list is empty, so you get the "Your messages" empty screen from your design. If you later pass real conversations, the same screen shows a chat list instead.

```kotlin
// ============================================================
// FILE:   home/MessagesScreen.kt
// ACTION: CREATE A NEW FILE in the "home" folder
//         (right-click home > New > Kotlin Class/File > MessagesScreen)
// ============================================================
package com.example.dugo.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.login.Brand

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)

/** One chat in the list. Connect this to your backend later. */
data class Conversation(
    val id: Int,
    val name: String,
    val bloodType: String,
    val lastMessage: String,
    val time: String,
    val unread: Boolean = false
)

@Composable
fun MessagesScreen(
    roleLabel: String = "Blood Donor",
    conversations: List<Conversation> = emptyList(),
    selectedTab: HomeTab = HomeTab.Messages,
    onTabSelected: (HomeTab) -> Unit = {},
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    onOpenConversation: (Conversation) -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .statusBarsPadding()
    ) {
        Column(Modifier.background(Color.White)) {
            HomeHeader(roleLabel, onNotifications, onProfile)
        }
        HorizontalDivider(color = Line)

        if (conversations.isEmpty()) {
            EmptyMessages(Modifier.weight(1f).fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text("Messages", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text("Chats with donors and blood seekers", fontSize = 13.sp, color = Gray)
                    }
                }
                items(conversations, key = { it.id }) { convo ->
                    ConversationRow(convo, onClick = { onOpenConversation(convo) })
                }
            }
        }

        HorizontalDivider(color = Line)
        Box(Modifier.background(Color.White)) { BottomNav(selectedTab, onTabSelected) }
    }
}

@Composable
private fun EmptyMessages(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFFFEFEF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.MailOutline,
                contentDescription = null,
                tint = Brand,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Your messages", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Conversations with donors and blood seekers will appear here.",
            fontSize = 12.sp,
            color = Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
private fun ConversationRow(convo: Conversation, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, Line, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEEF0F3)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Gray)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(convo.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("  •  ${convo.bloodType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Brand)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                convo.lastMessage,
                fontSize = 12.sp,
                color = if (convo.unread) Ink else Gray,
                fontWeight = if (convo.unread) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(convo.time, fontSize = 11.sp, color = Gray)
            if (convo.unread) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(Brand))
            }
        }
    }
}
```

> The empty-state icon uses the mail icon because your project only includes the core Material icons. The chat bubble in your design is in the extended icon set. For the exact bubble, add `androidx.compose.material:material-icons-extended` to `build.gradle.kts` and use `Icons.Outlined.ChatBubbleOutline`.

---

## STEP 2 – Edit `home/HomeScreenMap.kt` (just 2 words)

The Messages page reuses the header and bottom bar from the map screen, so they must not be `private`.

```kotlin
// ============================================================
// FILE:   home/HomeScreenMap.kt   (existing file)
// ACTION: remove the word "private" from these two functions
// ============================================================

// CHANGE THIS:   private fun HomeHeader(roleLabel: String, onNotifications: () -> Unit, onProfile: () -> Unit) {
// TO THIS:
@Composable
internal fun HomeHeader(roleLabel: String, onNotifications: () -> Unit, onProfile: () -> Unit) {

// CHANGE THIS:   private fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
// TO THIS:
@Composable
internal fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
```

(If you already did this for the Requests page, skip this step.)

---

## STEP 3 – Edit `MainActivity.kt`

```kotlin
// ============================================================
// FILE:   MainActivity.kt   (existing file)
// EDIT 3.1 – IMPORT
// ACTION: ADD this line to the import list at the top
// ============================================================
import com.example.dugo.home.MessagesScreen
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 3.2 – TAB STATE
// ACTION: ADD inside setContent > DuGOTheme { ... }
//         right UNDER the line   var roleLabel by remember { mutableStateOf("Blood Donor") }
//         (skip if you already have   var tab by remember { ... }   from the Requests page)
// ============================================================
                var tab by remember { mutableStateOf(HomeTab.Map) }
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 3.3 – HOME SCREEN (map tab + messages tab)
// ACTION: REPLACE the whole   Screen.Home -> HomeScreenMap( ... )   block
//         with this one
// ============================================================
                    Screen.Home -> {
                        // Map and Messages are real pages; Requests and Profile are still "coming soon"
                        val onTab: (HomeTab) -> Unit = { picked ->
                            if (picked == HomeTab.Map || picked == HomeTab.Messages) {
                                tab = picked
                            } else {
                                Toast.makeText(this, "${picked.label} coming soon", Toast.LENGTH_SHORT).show()
                            }
                        }
                        val onBell = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() }
                        val onProf = { Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show() }

                        if (tab == HomeTab.Messages) {
                            MessagesScreen(
                                roleLabel = roleLabel,
                                selectedTab = tab,
                                onTabSelected = onTab,
                                onNotifications = onBell,
                                onProfile = onProf
                                // conversations = emptyList() by default -> shows the "Your messages" screen.
                                // To preview the chat list, temporarily add:
                                // conversations = listOf(
                                //     com.example.dugo.home.Conversation(1, "Mia Santos", "O+", "Yes, I can donate today.", "2m", true),
                                //     com.example.dugo.home.Conversation(2, "Carlo Reyes", "A+", "Where is the hospital?", "1h")
                                // )
                            )
                        } else {
                            HomeScreenMap(
                                roleLabel = roleLabel,
                                selectedTab = tab,
                                onTabSelected = onTab,
                                onNotifications = onBell,
                                onProfile = onProf,
                                onMessageDonor = { donor ->
                                    Toast.makeText(this, "Messaging ${donor.name} coming soon", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
```

**Already added the Requests page?** Don't replace your block. Do two small changes instead:
1. In your `onTab`, also allow Messages: `if (picked == HomeTab.Map || picked == HomeTab.Requests || picked == HomeTab.Messages) { tab = picked }`
2. Add this branch to your `when (tab)` (or add an `else if (tab == HomeTab.Messages)` to your `if`), using the same `MessagesScreen(...)` call as above.

---

## HOW TO TEST

1. Run the app and log in with `admin` / `admin123`.
2. Tap the **Messages** tab. You should see the empty "Your messages" screen.
3. To preview the chat list, uncomment the `conversations = listOf(...)` lines in Step 3.3.

---

## Quick checklist of files

| Action | File |
|---|---|
| **Create** | `home/MessagesScreen.kt` |
| Edit | `home/HomeScreenMap.kt` (remove `private` from 2 functions) |
| Edit | `MainActivity.kt` (1 import, tab state, Home block) |
