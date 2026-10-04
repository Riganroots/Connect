package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun DeleteAccountButton(onDelete: (String, (String?) -> Unit) -> Unit) {
    var show by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    TextButton(onClick = { show = true; password = ""; error = null }) { Text("Delete account") }
    if (show) AlertDialog(
        onDismissRequest = { if (!busy) { show = false; password = "" } },
        title = { Text("Permanently delete your account?") },
        text = { Column {
            Text("This removes your profile, hosted activities and their chats, your other messages, memberships and linked reports. You will be signed out while server cleanup completes. This cannot be undone.")
            OutlinedTextField(value = password, onValueChange = { password = it },
                enabled = !busy, singleLine = true, label = { Text("Current password") },
                visualTransformation = PasswordVisualTransformation())
            error?.let { Text(it) }
        } },
        confirmButton = { TextButton(enabled = !busy && password.isNotBlank(), onClick = {
            busy = true; error = null
            onDelete(password) { result ->
                busy = false; password = ""
                if (result == null) show = false else error = result
            }
        }) { Text(if (busy) "Requesting…" else "Delete permanently") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { show = false; password = "" }) { Text("Cancel") } }
    )
}
