package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.viewmodel.validateActivityForm
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConnectViewModel

@Composable
fun CreatePlanScreen(
    viewModel: ConnectViewModel,
    onPlanPublished: () -> Unit
) {
    val title by viewModel.formTitle.collectAsStateWithLifecycle()
    val category by viewModel.formCategory.collectAsStateWithLifecycle()
    val location by viewModel.formLocation.collectAsStateWithLifecycle()
    val date by viewModel.formDate.collectAsStateWithLifecycle()
    val time by viewModel.formTime.collectAsStateWithLifecycle()
    val price by viewModel.formPrice.collectAsStateWithLifecycle()
    val participants by viewModel.formParticipantsNeeded.collectAsStateWithLifecycle()
    val description by viewModel.formDescription.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    val publishError by viewModel.publishError.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val pickDate = {
        val now = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(context, { _, year, month, day ->
            val chosen = java.util.Calendar.getInstance().apply { set(year, month, day) }
            viewModel.formDate.value = java.text.SimpleDateFormat("EEE, d MMM yyyy", java.util.Locale.ENGLISH).format(chosen.time)
        }, now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }
    val pickTime = {
        val now = java.util.Calendar.getInstance()
        android.app.TimePickerDialog(context, { _, hour, minute ->
            val chosen = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, minute) }
            viewModel.formTime.value = java.text.SimpleDateFormat("h:mm a", java.util.Locale.ENGLISH).format(chosen.time)
        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), false).show()
    }
    var showValidation by remember { mutableStateOf(false) }
    val validationError = validateActivityForm(
        title.trim(), location.trim(), date.trim(), time.trim(), price.trim(), participants, description.trim()
    )

    Column(modifier = Modifier.fillMaxSize().background(ConnectWhite).imePadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("Make a plan", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ConnectGrayDark)
                    Text(
                        "Choose what, where and when. Invite good company.",
                        fontSize = 14.sp,
                        color = ConnectGrayMedium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            item {
                FormSection("What are you planning?", "Use a short title people can understand at a glance.") {
                    ConnectTextField(
                        value = title,
                        onValueChange = { viewModel.formTitle.value = it },
                        label = "Activity title",
                        placeholder = "e.g. Saturday futsal in Baluwatar",
                        isError = showValidation && title.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                    Spacer(modifier = Modifier.height(7.dp))

                    val categories = listOf("Play", "Explore", "Meet", "Learn", "Experience")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(categories) { option ->
                            val selected = option == category
                            Surface(
                                modifier = Modifier.heightIn(min = 48.dp).clickable { viewModel.formCategory.value = option },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) ConnectDarkGreen else ConnectCream,
                                border = if (selected) null else BorderStroke(1.dp, ConnectGrayLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        option,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else ConnectGrayDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                FormSection("Where & when", "Pick a clear meeting point and a date.") {
                    ConnectTextField(
                        value = location,
                        onValueChange = { viewModel.formLocation.value = it },
                        label = "Meeting point",
                        placeholder = "e.g. Garden of Dreams, Thamel",
                        isError = showValidation && location.isBlank(),
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ConnectDarkGreen)
                        },
                        modifier = Modifier.testTag("form_location_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val areas = listOf("Thamel", "Patan", "Jhamsikhel", "Boudha", "Baluwatar", "Budhanilkantha")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(areas) { area ->
                            val selected = location.contains(area, ignoreCase = true)
                            AssistChip(
                                onClick = {
                                    if (!selected) {
                                        viewModel.formLocation.value =
                                            if (location.isBlank()) area else "$location, $area"
                                    }
                                },
                                label = { Text(area, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected) ConnectMint else ConnectWhite,
                                    labelColor = ConnectDarkGreen
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (selected) ConnectDarkGreen else ConnectGrayLight
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ConnectTextField(
                            value = date,
                            onValueChange = { viewModel.formDate.value = it },
                            label = "Date",
                            trailingIcon = { IconButton(onClick = { pickDate() }) { Icon(Icons.Default.DateRange, contentDescription = "Choose date", tint = ConnectDarkGreen) } },
                            placeholder = "e.g. 7 November 2026",
                            isError = showValidation && date.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ConnectTextField(
                            value = time,
                            onValueChange = { viewModel.formTime.value = it },
                            label = "Time (optional)",
                            trailingIcon = { IconButton(onClick = { pickTime() }) { Icon(Icons.Default.Edit, contentDescription = "Choose time", tint = ConnectDarkGreen) } },
                            placeholder = "7:30 PM",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                FormSection("Group details", "Keep it simple—these can be free or cost-shared.") {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ConnectTextField(
                            value = participants,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 5) {
                                    viewModel.formParticipantsNeeded.value = value
                                }
                            },
                            label = "People wanted",
                            placeholder = "4",
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectDarkGreen)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = showValidation && (participants.toIntOrNull()?.let { it in 1..10000 } != true)
                        )
                        ConnectTextField(
                            value = price,
                            onValueChange = { viewModel.formPrice.value = it },
                            label = "Cost per person",
                            placeholder = "Free",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                FormSection("Tell people what to expect", "Mention the vibe, what to bring, or who this is good for.") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.formDescription.value = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = {
                            Text(
                                "Example: Friendly beginner hike. Bring water and comfortable shoes. We'll meet at the gate and go together.",
                                fontSize = 14.sp
                            )
                        },
                        isError = showValidation && description.isBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = connectFieldColors(),
                        minLines = 4
                    )
                }
            }


        }

        Surface(color = ConnectWhite, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
                if (showValidation && validationError != null) {
                    Text(validationError, color = ConnectError, fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                publishError?.let { message ->
                    Text(message, color = ConnectError, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (isPublishing) {
                    Text("Saving activity… Stay connected. Your activity appears in Activities after confirmation.",
                        fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    enabled = !isPublishing,
                    onClick = {
                        showValidation = true
                        if (validationError == null) {
                            viewModel.publishActivityPlan(onSuccess = onPlanPublished)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_plan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
                ) {
                    Text(if (isPublishing) "Publishing…" else "Publish activity", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    if (currentUserId == "preview-user") {
                        "Preview Mode: this activity stays on this device only."
                    } else {
                        "This activity will be shared with signed-in Connect members."
                    },
                    fontSize = 12.sp,
                    color = ConnectGrayMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    helper: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ConnectGrayDark)
        Text(helper, style = MaterialTheme.typography.bodyMedium, color = ConnectGrayMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        content()
    }
}

@Composable
private fun ConnectTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        isError = isError,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(14.dp),
        colors = connectFieldColors()
    )
}

@Composable
private fun connectFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ConnectDarkGreen,
    unfocusedBorderColor = ConnectGrayLight,
    focusedContainerColor = ConnectMintLight,
    unfocusedContainerColor = ConnectMintLight,
    errorBorderColor = ConnectError,
    cursorColor = ConnectDarkGreen
)
