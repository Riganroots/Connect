package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.cloud.CloudReportRepository
import com.example.data.models.Plan
import com.example.ui.viewmodel.ConnectViewModel

@Composable
fun ReportActivityButton(viewModel: ConnectViewModel, plan: Plan?, planId: Long) {
    val currentUser by viewModel.currentUserId.collectAsState()
    if (plan == null || plan.cloudId.isBlank() || plan.organizerId == currentUser) return
    val context = LocalContext.current
    var showDialog by remember(planId, currentUser) { mutableStateOf(false) }
    var reason by remember(planId, currentUser) { mutableStateOf("") }
    var details by remember(planId, currentUser) { mutableStateOf("") }
    var submitting by remember(planId, currentUser) { mutableStateOf(false) }
    var error by remember(planId, currentUser) { mutableStateOf<String?>(null) }

    TextButton(onClick = { showDialog = true; error = null }) { Text("Report") }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!submitting) showDialog = false },
            title = { Text("Report activity") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Choose a reason. Your report is private and does not leave this activity or block its host.")
                    CloudReportRepository.REASONS.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !submitting) { reason = option }) {
                            RadioButton(selected = reason == option, onClick = { reason = option }, enabled = !submitting)
                            Text(option)
                        }
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it.take(1000) },
                        enabled = !submitting,
                        label = { Text("Details (optional)") },
                        supportingText = { Text("${details.length}/1000 · Avoid sharing sensitive personal information.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(enabled = !submitting && reason.isNotBlank(), onClick = {
                    submitting = true
                    error = null
                    viewModel.reportActivity(planId, reason, details) { result ->
                        submitting = false
                        if (result == null) {
                            showDialog = false
                            reason = ""
                            details = ""
                            Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_LONG).show()
                        } else {
                            error = result
                        }
                    }
                }) { Text(if (submitting) "Submitting…" else "Submit report") }
            },
            dismissButton = {
                TextButton(enabled = !submitting, onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
