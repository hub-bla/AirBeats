package pl.put.airbeats.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pl.put.airbeats.R
import com.google.firebase.auth.FirebaseAuth
import pl.put.airbeats.LocalUser
import pl.put.airbeats.routes.Screen
import pl.put.airbeats.ui.components.ErrorComponent
import pl.put.airbeats.ui.components.Loading


@Composable
fun LoginScreen(
    auth: FirebaseAuth, navController: NavController, modifier: Modifier = Modifier
) {
    val userState = LocalUser.current
    Column(
        modifier = modifier
            .fillMaxSize()
//            .wrapContentSize(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("marik@test.com") }
        var password by remember { mutableStateOf("marik123") }
        var passwordVisible by remember { mutableStateOf(false) }

        val login by rememberUpdatedState(newValue = {
            if (email.isEmpty() || password.isEmpty()) {
                return@rememberUpdatedState
            }
            isLoading = true
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("AirBeats", "signInWithEmail:success")
                        val userUID = auth.currentUser!!.uid
                        Log.d("AirBeats", "User logged in: $userUID")
                        userState.value = userUID
                        navController.navigate(Screen.Main.route)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("AirBeats", "signInWithEmail:failure", task.exception)
                        error = "Your login credentials don't match an account in our system."
                    }
                }
        })

        val register by rememberUpdatedState(newValue = {
            if (email.isEmpty() || password.isEmpty()) {
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

        Image(
            painter = painterResource(R.drawable.temporary_logo), "logo"
        )

        if (error != "") {
            ErrorComponent(
                error,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(0.6f)
            )
        }

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions()
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = Icons.Filled.Search
                // Please provide localized description for accessibility services
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            }
        )

        if (isLoading) {
            Loading()
            return
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Button(
                onClick = { login() },
                enabled = !isLoading
            ) {
                Text("Login")
            }

            Button(
                onClick = { register() },
                enabled = !isLoading
            ) {
                Text("Register")
            }
        }
    }

}

//@Preview
//    (
//    showBackground = true, showSystemUi = true
//)
//@Composable
//fun PreviewLogin() {
//    AirBeatsTheme {
//        LoginScreen(
//            auth = Firebase.auth,
//            modifier = Modifier
//        )
//    }
//}