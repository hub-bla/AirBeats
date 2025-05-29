package pl.put.airbeats.ui


import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import pl.put.airbeats.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import pl.put.airbeats.LocalUser
import pl.put.airbeats.routes.Screen
import pl.put.airbeats.ui.components.ErrorComponent
import pl.put.airbeats.ui.components.Loading
import pl.put.airbeats.utils.LottieLoading


@Composable
fun GoogleSignInButton(onSignIn: () -> Unit) {
    val userState = LocalUser.current
    val context = LocalContext.current

    var userInfo by remember { mutableStateOf<String?>(null) }
    var isSignedIn by remember { mutableStateOf(false) }
    val webClientId = context.getString(R.string.web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso).apply {
            signOut()
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            userInfo = "Welcome, ${user?.displayName}"
                            isSignedIn = true
                            val userUID = user!!.uid
                            Log.d("AirBeats", "User logged in: $userUID")
                            userState.value = userUID
                            onSignIn()
                        } else {
                            Log.e("GoogleSignIn", "Firebase auth failed", authTask.exception)
                            userInfo = "Auth failed"
                        }
                    }
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google sign-in failed", e)
            userInfo = "Google sign-in failed: ${e.localizedMessage}"
        }
    }

    Button(onClick = {
        signInLauncher.launch(googleSignInClient.signInIntent)
    }) {
        Text("Sign in with Google")
    }

}

@Composable
fun LoginScreen(
    auth: FirebaseAuth, navController: NavController, modifier: Modifier = Modifier
) {
    val userState = LocalUser.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("marik@test.com") }
        var password by remember { mutableStateOf("marik123") }
        var repeatedPassword by remember { mutableStateOf("") }
        var formType by remember { mutableStateOf("login") }

        val login by rememberUpdatedState(newValue = {
            if (email.isEmpty() || password.isEmpty()) {
                return@rememberUpdatedState
            }
            isLoading = true
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        Log.d("AirBeats", "signInWithEmail:success")
                        val userUID = auth.currentUser!!.uid
                        userState.value = userUID
                        navController.navigate(Screen.Main.route)
                    } else {
                        Log.w("AirBeats", "signInWithEmail:failure", task.exception)
                        error = "Your login credentials don't match an account in our system."
                    }
                }
        })


        val register by rememberUpdatedState(newValue = {
            if (email.isEmpty() || password.isEmpty()) {
                return@rememberUpdatedState
            }
            if (password != repeatedPassword) {
                error = "Passwords don't match"
                return@rememberUpdatedState
            }
            Log.d("AirBeats", "username: $email")
            Log.d("AirBeats", "password: $password")
            // Email and password requirements TODO
            isLoading = true
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("AirBeats", "createUserWithEmail:success")
                        val userUID = auth.currentUser!!.uid
                        Log.d("AirBeats", "New user registered: $userUID")
                        userState.value = userUID
                        navController.navigate(Screen.Main.route)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("AirBeats", "createUserWithEmail:failure", task.exception)
                        error = task.exception?.message.toString()
                    }
                }
        })
        Box(modifier = Modifier.padding(top = 50.dp)) {
            Image(
                painter = painterResource(R.drawable.airdrums_logo), "logo",
                modifier = Modifier.size(256.dp),
                contentScale = ContentScale.Fit
            )
        }

        if (error != "") {
            ErrorComponent(
                error,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(0.6f)
            )
        }

        if (isLoading) {
            LottieLoading(modifier = Modifier.fillMaxSize())
            return
        }

        when (formType) {
            "login" -> Login(
                email,
                password,
                { newEmailVal -> email = newEmailVal },
                { newPasswordVal -> password = newPasswordVal },
                isLoading,
                login,
                { formType = "register" }
            )

            "register" -> Register(
                email,
                password,
                repeatedPassword,
                { newEmailVal -> email = newEmailVal },
                { newPasswordVal -> password = newPasswordVal },
                { newRepPasswordVal -> repeatedPassword = newRepPasswordVal },
                isLoading,
                register,
                { formType = "login" }
            )
        }
        GoogleSignInButton({ navController.navigate(Screen.Main.route) })
    }

}

@Composable
fun PasswordInput(label: String, value: String, changeValue: (String) -> Unit) {
    var passwordVisible = remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = { changeValue(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = Icons.Filled.Search
            // Please provide localized description for accessibility services
            val description = if (passwordVisible.value) "Hide password" else "Show password"

            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                Icon(imageVector = image, description)
            }
        }
    )
}


@Composable
fun SignInForm(
    email: String,
    password: String,
    changeEmailVal: (String) -> Unit,
    changePasswordVal: (String) -> Unit,
) {
    TextField(
        value = email,
        onValueChange = { changeEmailVal(it) },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions()
    )

    PasswordInput("Password", password, changePasswordVal)
}


@Composable
fun Login(
    email: String,
    password: String,
    changeEmailVal: (String) -> Unit,
    changePasswordVal: (String) -> Unit,
    isLoading: Boolean,
    handleLogin: () -> Unit,
    changeToRegisterForm: () -> Unit
) {
    SignInForm(
        email,
        password,
        changeEmailVal,
        changePasswordVal,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { handleLogin() },
            enabled = !isLoading
        ) {
            Text("Login")
        }

        Text(
            text = "Create account",
            modifier = Modifier.clickable { changeToRegisterForm() },
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline
        )
    }

}

@Composable
fun Register(
    email: String,
    password: String,
    repeatedPassword: String,
    changeEmailVal: (String) -> Unit,
    changePasswordVal: (String) -> Unit,
    changeRepPassVal: (String) -> Unit,
    isLoading: Boolean,
    handleRegister: () -> Unit,
    changeToLoginForm: () -> Unit
) {
    SignInForm(
        email,
        password,
        changeEmailVal,
        changePasswordVal,
    )

    PasswordInput("Repeat password", repeatedPassword, changeRepPassVal)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { handleRegister() },
            enabled = !isLoading
        ) {
            Text("Register")
        }

        Text(
            text = "Sign In",
            modifier = Modifier.clickable { changeToLoginForm() },
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline
        )
    }
}