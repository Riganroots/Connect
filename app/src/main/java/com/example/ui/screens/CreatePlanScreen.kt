package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

    var showValidation by remember { mutableStateOf(false) }
    val missingRequired = title.isBlank() || location.isBlank() || date.isBlank() || description.isBlank()

    Column(modifier = Modifier.fillMaxSize().background(ConnectMintLight)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("Create an activity", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ConnectGrayDark)
                    Text(
                        "A few details are enough. You can coordinate the rest in chat.",
                        fontSize = 11.sp,
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
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                    Spacer(modifier = Modifier.height(7.dp))

                    val categories = listOf("Play", "Explore", "Meet", "Learn", "Experience")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(categories) { option ->
                            val selected = option == category
                            Surface(
                                modifier = Modifier.clickable { viewModel.formCategory.value = option },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) ConnectDarkGreen else ConnectCream,
                                border = if (selected) null else BorderStroke(1.dp, ConnectGrayLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        option,
                                        fontSize = 10.sp,
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
                                label = { Text(area, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        ConnectTextField(
                            value = date,
                            onValueChange = { viewModel.formDate.value = it },
                            label = "Date",
                            placeholder = "Sat, Jun 6",
                            isError = showValidation && date.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        ConnectTextField(
                            value = time,
                            onValueChange = { viewModel.formTime.value = it },
                            label = "Time",
                            placeholder = "7:30 PM",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                FormSection("Group details", "Keep it simple—these can be free or cost-shared.") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        ConnectTextField(
                            value = participants,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 3) {
                                    viewModel.formParticipantsNeeded.value = value
                                }
                            },
                            label = "People wanted",
                            placeholder = "4",
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectDarkGreen)
                            },
                            modifier = Modifier.weight(0.42f)
                        )
                        ConnectTextField(
                            value = price,
                            onValueChange = { viewModel.formPrice.value = it },
                            label = "Cost per person",
                            placeholder = "Free",
                            modifier = Modifier.weight(0.58f)
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
                                fontSize = 11.sp
                            )
                        },
                        isError = showValidation && description.isBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = connectFieldColors(),
                        minLines = 4
                    )
                }
            }

            if (showValidation && missingRequired) {
                item {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFCC80))
                    ) {
                        Text(
                            "Please add a title, meeting point, date and short description.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF8A4B00),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Surface(color = ConnectWhite, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
                Button(
                    onClick = {
                        showValidation = true
                        if (!missingRequired) {
                            viewModel.publishActivityPlan(onSuccess = onPlanPublished)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_plan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
                ) {
                    Text("Publish activity", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    "Your plan is currently saved on this device.",
                    fontSize = 9.sp,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        border = BorderStroke(1.dp, ConnectGrayLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ConnectGrayDark)
            Text(helper, fontSize = 10.sp, color = ConnectGrayMedium, modifier = Modifier.padding(top = 2.dp, bottom = 11.dp))
            content()
        }
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
    leadingIcon: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        placeholder = { Text(placeholder, fontSize = 10.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        leadingIcon = leadingIcon,
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
