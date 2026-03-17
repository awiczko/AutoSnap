package com.example.autosnap_inz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.firestore.FirebaseFirestore

class LogInActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)

        Log.d("LogInActivity", "Current user: ${auth.currentUser}")

        //  Sprawdzenie czy użytkownik już jest zalogowany
        if (auth.currentUser != null) {
            Log.d("LogInActivity", "User is already logged in, going to CameraActivity")
            // przejście do MainActivity
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
            return
        }

        setContent {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                val credential = try {
                    oneTapClient.getSignInCredentialFromIntent(result.data)
                } catch (e: Exception) {
                    Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    null
                }

                credential?.googleIdToken?.let { idToken ->
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Welcome ${auth.currentUser?.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            // utworzenie nowego dokumentu
                            auth.currentUser?.uid?.let { uid ->
                                createUserIfNotExists(uid)
                            }
                            // Przejście do MainActivity po zalogowaniu
                            startActivity(Intent(this, CameraActivity::class.java)) /// TUTAJ ZMIANA 21.09
                            finish()
                        } else {
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            LoginScreen(
                onGoogleSignInClick = {
                    val signInRequest = BeginSignInRequest.builder()
                        .setGoogleIdTokenRequestOptions(
                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                        )
                        .build()

                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            launcher.launch(
                                IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                            )
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Google Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            )
        }
    }
}

private fun createUserIfNotExists(uid: String) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("UserScore")
        .document("UserScore")      // dokument nadrzędny
        .collection("test")
        .document(uid)

    userRef.get().addOnSuccessListener { document ->
        if (!document.exists()) {
            val newUserData = mapOf(
                "points" to 0L // domyslne punkty
            )
            userRef.set(newUserData)
        }
    }
}
