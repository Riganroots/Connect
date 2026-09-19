package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.auth.AuthViewModel
import com.example.ui.theme.*

@Composable
fun AuthGate(
    viewModel: AuthViewModel,
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.canEnterApp) {
        content()
    } else {
        AuthScreen(
            isFirebaseConfigured = state.isFirebaseConfigured,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            infoMessage = state.infoMessage,
            onSignIn = viewModel::signIn,
            onCreateAccount = viewModel::createAccount,
            onResetPassword = viewModel::sendPasswordReset,
            onPreview = viewModel::enterPreviewMode,
            onClearMessage = viewModel::clearMessage
        )
    }
}

@Composable
private fun AuthScreen(
    isFirebaseConfigured: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onPreview: () -> Unit,
    onClearMessage: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createMode by remember { mutableStateOf(false) }

    LaunchedEffect(email, password, createMode) {
        if (errorMessage != null || infoMessage != null) {
            onClearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(ConnectDarkGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connect",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ConnectDarkGreen
            )
            Text(
                text = "Find people. Make plans. Go together.",
                fontSize = 12.sp,
                color = ConnectGrayMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ConnectWhite),
                border = BorderStroke(1.dp, ConnectGrayLight)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuthModeButton(
                            text = "Sign in",
                            selected = !createMode,
                            onClick = { createMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        AuthModeButton(
                            text = "Create account",
                            selected = createMode,
                            onClick = { createMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ConnectDarkGreen)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectDarkGreen)
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (!createMode) {
                        TextButton(
                            onClick = { onResetPassword(email) },
                            enabled = !isLoading && isFirebaseConfigured,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?", color = ConnectDarkGreen, fontSize = 11.sp)
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                fontSize = 10.sp,
                                color = ConnectError
                            )
                        }
                    }

                    if (infoMessage != null) {
                        Surface(
                            color = ConnectMint,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = infoMessage,
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                fontSize = 10.sp,
                                color = ConnectDarkGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (createMode) onCreateAccount(email, password)
                            else onSignIn(email, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isLoading && isFirebaseConfigured,
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (createMode) "Create account" else "Sign in",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (!isFirebaseConfigured) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Firebase setup required",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "Real sign-in is disabled until this Android app is registered in Firebase and its configuration file is added.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = ConnectGrayMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onPreview,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, ConnectDarkGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen)
                    ) {
                        Text("Continue in Preview Mode", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Preview Mode is for development only and is never a real signed-in account.",
                        fontSize = 9.sp,
                        color = ConnectGrayMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
        ) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, ConnectGrayLight)
        ) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
        }
    }
}
