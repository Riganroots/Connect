package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ConnectViewModel

@Composable
fun BlockUserButton(viewModel: ConnectViewModel, uid: String, name: String) {
    val currentUser by viewModel.currentUserId.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    if (uid.isBlank() || uid == currentUser || currentUser == "preview-user") return
    val blocked = uid in blockedUsers
    var dialog by remember(uid, currentUser) { mutableStateOf(false) }
    var busy by remember(uid, currentUser) { mutableStateOf(false) }
    var error by remember(uid, currentUser) { mutableStateOf<String?>(null) }
    TextButton(onClick = { dialog = true; error = null }) { Text(if (blocked) "Unblock" else "Block") }
    if (dialog) AlertDialog(
        onDismissRequest = { if (!busy) dialog = false },
        title = { Text(if (blocked) "Unblock $name?" else "Block $name?") },
        text = { Column {
            Text(if (blocked) "Their activities, availability and chat messages will be shown again." else
                "Hide their activities, availability and chat messages for your account. You stay joined to shared activities. This does not submit a report.")
            error?.let { Text(it) }
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            viewModel.setUserBlocked(uid, name, !blocked) { result ->
                busy = false
                if (result == null) dialog = false else error = result
            }
        }) { Text(if (busy) "Updating…" else if (blocked) "Unblock" else "Block user") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { dialog = false }) { Text("Cancel") } }
    )
}

@Composable
fun BlockedUsersManager(viewModel: ConnectViewModel) {
    val users by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val syncError by viewModel.blockError.collectAsStateWithLifecycle()
    var show by remember { mutableStateOf(false) }
    TextButton(onClick = { show = true }) { Text("Blocked users (${users.size})") }
    if (show) AlertDialog(
        onDismissRequest = { show = false },
        title = { Text("Blocked users") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            syncError?.let { Text(it) }
            if (users.isEmpty()) Text("No blocked users.")
            users.forEach { (uid, name) ->
                Row { Text(name, modifier = Modifier.weight(1f)); BlockUserButton(viewModel, uid, name) }
            }
        } },
        confirmButton = { TextButton(onClick = { show = false }) { Text("Done") } }
    )
}
