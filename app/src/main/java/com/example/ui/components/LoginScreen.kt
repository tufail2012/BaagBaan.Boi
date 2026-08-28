package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun LoginScreen(
    onLoginSuccess: (userEmail: String) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmailInput by remember { mutableStateOf("") }
    var forgotSuccessMessage by remember { mutableStateOf<String?>(null) }

    var isSignUpMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var verificationEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isSignUpMode) {
        errorMessage = null
    }

    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() ?: context }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    fun getAuth() = com.example.util.SafeFirebase.getAuth(context)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        com.example.util.SafeFirebase.logTrace("googleSignInLauncher result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    com.example.util.SafeFirebase.logTrace("GoogleSignIn idToken received, signing into Firebase...")
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val auth = getAuth()
                    if (auth != null) {
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { signInTask ->
                                isGoogleLoading = false
                                if (signInTask.isSuccessful) {
                                    com.example.util.SafeFirebase.logTrace("Firebase sign-in succeeded")
                                    val user = signInTask.result?.user ?: auth.currentUser
                                    onLoginSuccess(user?.email ?: "google.user@gmail.com")
                                } else {
                                    com.example.util.SafeFirebase.logTrace("Firebase sign-in failed: ${signInTask.exception?.message}")
                                    errorMessage = "${signInTask.exception?.localizedMessage ?: "Google sign-in failed"}\n\n${com.example.util.SafeFirebase.getTraceString()}"
                                }
                            }
                    } else {
                        isGoogleLoading = false
                        val err = com.example.util.SafeFirebase.lastAuthError ?: com.example.util.SafeFirebase.lastInitError
                        val baseErr = if (err != null) "Firebase Auth Error: ${err.javaClass.simpleName}: ${err.message}" else "Firebase Authentication is unavailable on this device."
                        errorMessage = "$baseErr\n\n${com.example.util.SafeFirebase.getTraceString()}"
                    }
                } else {
                    isGoogleLoading = false
                    errorMessage = "Failed to retrieve Google ID token from sign-in result.\n\n${com.example.util.SafeFirebase.getTraceString()}"
                }
            } catch (e: ApiException) {
                isGoogleLoading = false
                com.example.util.SafeFirebase.logTrace("GoogleSignIn ApiException code: ${e.statusCode}, message: ${e.message}")
                if (e.statusCode == 12501) { // Sign-in cancelled by user
                    com.example.util.SafeFirebase.logTrace("Google sign-in cancelled by user")
                } else {
                    errorMessage = "Google sign-in error (code ${e.statusCode}): ${e.localizedMessage ?: "Sign-in failed"}\n\n${com.example.util.SafeFirebase.getTraceString()}"
                }
            } catch (e: Exception) {
                isGoogleLoading = false
                com.example.util.SafeFirebase.logTrace("GoogleSignIn Exception: ${e.javaClass.simpleName}: ${e.message}")
                errorMessage = "${e.localizedMessage ?: "Google sign-in failed"}\n\n${com.example.util.SafeFirebase.getTraceString()}"
            }
        } else {
            isGoogleLoading = false
            com.example.util.SafeFirebase.logTrace("Google sign-in cancelled or failed result code: ${result.resultCode}")
        }
    }

    // ============================================================
    // CRITICAL: Google Sign-In trigger function
    // ============================================================
    val performGoogleSignIn: () -> Unit = performGoogleSignIn@ {
        if (isGoogleLoading) return@performGoogleSignIn
        isGoogleLoading = true
        errorMessage = null

        com.example.util.SafeFirebase.clearTrace()
        com.example.util.SafeFirebase.logTrace("performGoogleSignIn started")

        val webClientId = "858579936461-usbgrgcsf6tlko3ga91nnlaud874dp1g.apps.googleusercontent.com"
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val serverClientId = if (webClientIdResId != 0) {
            val resValue = context.getString(webClientIdResId)
            if (resValue.isNotBlank()) resValue else webClientId
        } else {
            webClientId
        }

        try {
            com.example.util.SafeFirebase.logTrace("Launching GoogleSignIn intent with serverClientId: $serverClientId")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(client.signInIntent)
        } catch (e: Exception) {
            isGoogleLoading = false
            com.example.util.SafeFirebase.logTrace("Failed to launch GoogleSignIn intent: ${e.message}")
            errorMessage = "${e.localizedMessage ?: "Google sign-in failed"}\n\n${com.example.util.SafeFirebase.getTraceString()}"
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (verificationEmail != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Verification",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Verify Your Email",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "We have sent you a verification email to $verificationEmail. Please verify it and log in.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp),
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            email = verificationEmail ?: email
                            verificationEmail = null
                            isSignUpMode = false
                            password = ""
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verification_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Header Row with Guest Back option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onContinueAsGuest,
                        modifier = Modifier.testTag("login_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Main",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onContinueAsGuest,
                        modifier = Modifier.testTag("skip_login_button")
                    ) {
                        Text(
                            text = "Skip • Guest Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Logo & Title Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppBrandLogo(
                        size = 84.dp,
                        shape = RoundedCornerShape(24.dp),
                        contentDescription = "Baagbaan Boi Logo"
                    )

                    Text(
                        text = "Baagbaan Boi",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("app_logo_title")
                    )

                    Text(
                        text = "Orchard & Horticulture Management System",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Login Card Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "Create Account" else "Welcome Back",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("Enter your name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. farmer@baagbaan.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = elevatedInputFieldColors(accentColor = MaterialTheme.colorScheme.primary)
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = elevatedInputFieldColors(accentColor = MaterialTheme.colorScheme.primary)
                        )

                        // Forgot Password Link
                        if (!isSignUpMode) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable {
                                            forgotEmailInput = email
                                            showForgotPasswordDialog = true
                                        }
                                        .testTag("forgot_password_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Primary Login / Sign Up Button
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter email and password"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                val cleanEmail = email.trim()
                                val auth = getAuth()

                                if (auth == null) {
                                    isLoading = false
                                    val err = com.example.util.SafeFirebase.lastAuthError ?: com.example.util.SafeFirebase.lastInitError
                                    val baseErr = if (err != null) "Firebase Auth Error: ${err.javaClass.simpleName}: ${err.message}" else "Firebase Authentication is unavailable on this device."
                                    errorMessage = "$baseErr\n\n${com.example.util.SafeFirebase.getTraceString()}"
                                    return@Button
                                }

                                if (isSignUpMode) {
                                    val nameToSave = fullName.trim()
                                    auth.createUserWithEmailAndPassword(cleanEmail, password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                val user = task.result?.user ?: auth.currentUser
                                                if (user != null && nameToSave.isNotEmpty()) {
                                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(nameToSave)
                                                        .build()
                                                    user.updateProfile(profileUpdates)
                                                }
                                                user?.sendEmailVerification()
                                                auth.signOut()
                                                verificationEmail = cleanEmail
                                            } else {
                                                val exception = task.exception
                                                if (exception is FirebaseAuthUserCollisionException) {
                                                    errorMessage = "User already exists. Please sign in"
                                                } else {
                                                    val msg = exception?.message ?: ""
                                                    if (msg.contains("already in use", ignoreCase = true) || msg.contains("exists", ignoreCase = true)) {
                                                        errorMessage = "User already exists. Please sign in"
                                                    } else if (password.length < 6) {
                                                        errorMessage = "Password should be at least 6 characters"
                                                    } else {
                                                        errorMessage = exception?.localizedMessage ?: "Sign up failed"
                                                    }
                                                }
                                            }
                                        }
                                } else {
                                    auth.signInWithEmailAndPassword(cleanEmail, password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                val user = task.result?.user ?: auth.currentUser
                                                if (user != null && !user.isEmailVerified) {
                                                    user.sendEmailVerification()
                                                    auth.signOut()
                                                    verificationEmail = user.email ?: cleanEmail
                                                } else {
                                                    onLoginSuccess(user?.email ?: cleanEmail)
                                                }
                                            } else {
                                                errorMessage = "Email or password is incorrect"
                                            }
                                        }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_primary_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) "Create Account" else "Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Divider OR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "OR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Sign in with Google Button
                        OutlinedButton(
                            onClick = { performGoogleSignIn() },
                            enabled = !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom Google "G" Badge Icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "G",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF4285F4)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Toggle mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isSignUpMode) "Log In" else "Sign Up",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                isSignUpMode = !isSignUpMode
                                errorMessage = null
                            }
                            .testTag("toggle_signup_login_button")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                forgotSuccessMessage = null
            },
            title = {
                Text("Reset Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your registered email address below. We'll send you a password reset link.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = forgotEmailInput,
                        onValueChange = { forgotEmailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_email_input")
                    )

                    if (forgotSuccessMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = forgotSuccessMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanEmail = forgotEmailInput.trim()
                        if (cleanEmail.isBlank()) {
                            forgotSuccessMessage = "Please enter an email address"
                            return@Button
                        }
                        val auth = getAuth()
                        if (auth == null) {
                            forgotSuccessMessage = "Authentication service is unavailable."
                            return@Button
                        }
                        auth.sendPasswordResetEmail(cleanEmail)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    forgotSuccessMessage = "Password reset link sent to $cleanEmail"
                                } else {
                                    forgotSuccessMessage = task.exception?.localizedMessage ?: "Failed to send reset email"
                                }
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("send_reset_link_button")
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        forgotSuccessMessage = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

