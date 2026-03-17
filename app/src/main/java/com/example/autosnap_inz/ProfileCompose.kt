package com.example.autosnap_inz

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.autosnap_inz.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage

import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

@Composable
fun ProfileScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var profilEmail by remember { mutableStateOf("Ładowanie...") }
    var profilNickname by remember { mutableStateOf("Ładowanie...") }
    var profilScore by remember { mutableStateOf("Ładowanie...") }
    var achievementCount by remember { mutableStateOf("Ładowanie...") }
    var profileImageSource by remember { mutableStateOf<Any?>(R.drawable.user) }

    // Pobranie kontekstu, aby móc uruchomić nową Aktywność
    val context = LocalContext.current

    // Pobranie danych z Firestore
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        user?.let {
            val uid = it.uid
            profilEmail = it.email ?: "Brak emaila"
            profileImageSource = it.photoUrl?.toString() ?: R.drawable.user

            val googleNick = it.displayName?.ifEmpty { null }
            val emailNick = profilEmail.substringBefore("@")
            val defaultNick = googleNick ?: emailNick

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    profilNickname = document?.getString("nickname") ?: defaultNick
                }
                .addOnFailureListener {
                    profilNickname = defaultNick
                }

            db.collection("AutoSnapInz")
                .document("UserScore")
                .collection("test")
                .document(uid)
                .addSnapshotListener { scoreDocument, error ->
                    if (error != null) {
                        profilScore = "Błąd"
                        achievementCount = "Błąd"
                        return@addSnapshotListener
                    }

                    if (scoreDocument != null && scoreDocument.exists()) {
                        val score = scoreDocument.getLong("points") ?: 0
                        profilScore = score.toString()

                        db.collection("AutoSnapInz")
                            .document("Achievements")
                            .get()
                            .addOnSuccessListener { achDoc ->
                                if (achDoc != null && achDoc.exists()) {
                                    val requiredPointsList = achDoc.data?.mapNotNull { entry ->
                                        (entry.value as? Long) ?: 0L
                                    } ?: emptyList()

                                    val count = requiredPointsList.filter { pointsRequired ->
                                        pointsRequired <= score
                                    }.size

                                    achievementCount = count.toString()
                                } else {
                                    achievementCount = "0"
                                }
                            }
                            .addOnFailureListener {
                                achievementCount = "Błąd"
                            }
                    } else {
                        profilScore = "0"
                        achievementCount = "0"
                    }
                }
        }
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 15.dp, horizontal = 1.dp)


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(id = R.color.grey),
    ) { paddingValues -> // 'paddingValues' zawiera informacje o marginesach


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .padding(top = 1.dp, bottom = 1.dp)
                            .border(width = 4.dp, color = Color.Black, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = profileImageSource,
                            contentDescription = "Profile Picture",
                            placeholder = painterResource(id = R.drawable.user),
                            error = painterResource(id = R.drawable.user),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // Nick
                    Card(
                        modifier = cardModifier,
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey2))
                    ) {
                        ProfileInfoItem("Nick:", profilNickname, Modifier.padding(10.dp).height(40.dp))
                    }

                    //Email
                    Card(
                        modifier = cardModifier,
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey2))
                    ) {
                        ProfileInfoItem("Email:", profilEmail, Modifier.padding(10.dp).height(40.dp))
                    }

                    //Liczba punktów
                    Card(
                        modifier = cardModifier,
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey2))
                    ) {
                        ProfileInfoItem("Liczba punktów:", profilScore, Modifier.padding(10.dp).height(40.dp))
                    }

                    // Liczba odblokowanych osiągnięć
                    Card(
                        modifier = cardModifier,
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey2))
                    ) {
                        ProfileInfoItem("Osiągnięcia:", achievementCount, Modifier.padding(10.dp).height(40.dp))
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, MapActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                containerColor = colorResource(id = R.color.blue),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Pokaż mapę"
                )
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hum777Font: FontFamily = FontFamily(Font(R.font.hum777k))
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = hum777Font,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = value,
            color = colorResource(id = R.color.blue),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
