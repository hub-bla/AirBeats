package pl.put.airbeats.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.put.airbeats.R
import pl.put.airbeats.ui.theme.AirBeatsTheme
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.Firebase

@Composable
fun LoginScreen(userToken: String
                , updateUserToken: (String) -> Unit
                , modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(top = 20.dp)
        ,verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val db = Firebase.firestore

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        fun login() {
            // TODO
            db.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        Log.d(TAG, "${document.id} => ${document.data}")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                }
            Log.d("AirBeats","username: $username")
            Log.d("AirBeats","password: $password")
            updateUserToken(userToken)
            Log.d("AirBeats","userToken: $userToken")
        }
        fun register() {
            // TODO
            Log.d("AirBeats","username: $username")
            Log.d("AirBeats","password: $password")
            Log.d("AirBeats","userToken: $userToken")
        }

        Image(painter = painterResource(R.drawable.temporary_logo)
            , "logo"
        )

        TextField(value =  username
            , onValueChange = {username = it}
            , label = { Text("Username") }
            , singleLine = true
            , keyboardOptions = KeyboardOptions()
        )

        TextField(value =  password
            , onValueChange = {password = it}
            , label = { Text("Password") }
            , singleLine = true
            , keyboardOptions = KeyboardOptions()
        )

        Row (modifier = Modifier
            .align(Alignment.CenterHorizontally)
            , horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Button(onClick = ::login) {
                Text("Login")
            }

            Button(onClick = ::register) {
                Text("Register")
            }
        }
    }
}

@Preview
(   showBackground = true
//,   showSystemUi = true
)
@Composable
fun PreviewLogin() {
    AirBeatsTheme {
        LoginScreen("",{ Log.d("AirBeats","userToken changed to: $it")})
    }
}