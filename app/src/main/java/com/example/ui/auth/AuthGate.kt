package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.ui.components.ActivityPhotography
import com.example.BuildConfig
import com.example.auth.AuthViewModel
import com.example.ui.theme.*

data class ActiveAppSession(
    val userId: String,
    val suggestedName: String,
    val isPreviewMode: Boolean
)

@Composable
fun AuthGate(
    viewModel: AuthViewModel,
    content: @Composable (ActiveAppSession) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.canEnterApp) {
        val session = if (state.isPreviewMode) {
            ActiveAppSession(
                userId = "preview-user",
                suggestedName = "Preview User",
                isPreviewMode = true
            )
        } else {
            ActiveAppSession(
                userId = state.user?.uid.orEmpty(),
                suggestedName = state.user?.displayName?.takeIf { it.isNotBlank() } ?: "Connect Member",
                isPreviewMode = false
            )
        }
        content(session)
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
internal fun AuthScreen(
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
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createMode by rememberSaveable { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val submit = {
        if (!isLoading && isFirebaseConfigured) {
            focusManager.clearFocus()
            if (createMode) onCreateAccount(email, password) else onSignIn(email, password)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("connect", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark,
                letterSpacing = (-0.8).sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(16.dp)) {
                ActivityPhotography("Meet", Modifier.fillMaxWidth().height(144.dp))
            }
            Text("Good plans. Better company.", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold,
                color = ConnectGrayDark, modifier = Modifier.align(Alignment.Start).padding(top = 20.dp))
            Text("Meet people through the things you like to do.", fontSize = 14.sp, lineHeight = 20.sp,
                color = ConnectGrayMedium, modifier = Modifier.align(Alignment.Start).padding(top = 6.dp, bottom = 16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuthModeButton(
                            text = "Sign in",
                            selected = !createMode,
                            onClick = { createMode = false; onClearMessage() },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        )
                        AuthModeButton(
                            text = "Create account",
                            selected = createMode,
                            onClick = { createMode = true; onClearMessage() },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; onClearMessage() },
                        enabled = !isLoading,
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
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; onClearMessage() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectDarkGreen)
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }, enabled = !isLoading) {
                                Text(if (showPassword) "Hide" else "Show")
                            }
                        },
                        supportingText = {
                            if (createMode) Text("Use at least 6 characters.")
                        },
                        keyboardActions = KeyboardActions(onDone = { submit() }),
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
                            Text("Forgot password?", color = ConnectDarkGreen, fontSize = 14.sp)
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
                                fontSize = 14.sp,
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
                                fontSize = 14.sp,
                                color = ConnectDarkGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { submit() },
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "Real sign-in is disabled until this Android app is registered in Firebase and its configuration file is added.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
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
                        fontSize = 14.sp,
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
        ) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, ConnectGrayLight)
        ) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ConnectGrayDark)
        }
    }
}
