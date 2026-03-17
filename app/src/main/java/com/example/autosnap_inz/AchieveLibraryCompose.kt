package com.example.autosnap_inz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autosnap_inz.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AchievementsScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val userPointsState = remember { mutableStateOf(0L) }
    val achievementsState = remember { mutableStateOf(listOf<Achievement>()) }
    val placeholderBitmap = remember { BitmapFactory.decodeResource(context.resources, R.drawable.user) }

    val uid = auth.currentUser?.uid ?: return

    //Listener punktów użytkownika w czasie rzeczywistym
    DisposableEffect(uid) {
        val listener = db.collection("AutoSnapInz")
            .document("UserScore")
            .collection("test")
            .document(uid)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    userPointsState.value = document.getLong("points") ?: 0

                    // Pobieranie wszystkie osiągnięcia z dokumentu Achievements
                    db.collection("AutoSnapInz")
                        .document("Achievements")
                        .get()
                        .addOnSuccessListener { achDoc ->
                            if (achDoc != null && achDoc.exists()) {
                                val list = achDoc.data?.mapNotNull { entry ->
                                    val title = entry.key
                                    val pointsRequired = (entry.value as? Long) ?: 0L
                                    Achievement(
                                        title = title,
                                        description = "Osiągnięcie za ${pointsRequired} punktów",
                                        image = placeholderBitmap
                                    ) to pointsRequired
                                } ?: emptyList()

                                //  Filtrowanie osiągnięc według punktów użytkownika
                                achievementsState.value =
                                    list.filter { it.second <= userPointsState.value }.map { it.first }
                            } else {
                                achievementsState.value = emptyList()
                            }
                        }
                        .addOnFailureListener {
                            achievementsState.value = emptyList()
                        }
                } else {
                    userPointsState.value = 0
                    achievementsState.value = emptyList()
                }
            }

        onDispose { listener.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F1C1C))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth(0.7f)
                        .height(80.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FA))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Osiągnięcia",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievementsState.value) { achievement ->
                    AchievementItemCompose(
                        title = achievement.title,
                        description = achievement.description,
                        bitmap = achievement.image
                    )
                }
            }
        }
    }
}



//// Funkcja pomocnicza zwracająca przykładowe osiągnięcia
//private fun getSampleAchievements(context: Context): List<Achievement> {
//    val sampleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user)
//    return listOf(
//        Achievement("Pierwsze kroki", "Dodaj pierwsze zdjęcie", sampleBitmap),
//        Achievement("Eksplorator", "Odwiedź 10 różnych lokalizacji", sampleBitmap),
//        Achievement("Kolekcjoner", "Dodaj 50 zdjęć", sampleBitmap),
//        Achievement("Pierwsze kroki", "Dodaj pierwsze zdjęcie", sampleBitmap),
//        Achievement("Eksplorator", "Odwiedź 10 różnych lokalizacji", sampleBitmap),
//        Achievement("Kolekcjoner", "Dodaj 50 zdjęć", sampleBitmap),
//        Achievement("Pierwsze kroki", "Dodaj pierwsze zdjęcie", sampleBitmap),
//        Achievement("Eksplorator", "Odwiedź 10 różnych lokalizacji", sampleBitmap),
//        Achievement("Kolekcjoner", "Dodaj 50 zdjęć", sampleBitmap)
//    )
//}