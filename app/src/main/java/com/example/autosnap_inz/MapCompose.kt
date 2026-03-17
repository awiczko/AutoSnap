package com.example.autosnap_inz

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val db = FirebaseFirestore.getInstance()

    var locations by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val polandCenter = LatLng(52.2297, 21.0122)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(polandCenter, 6f)
    }

    // Lista marek do wyboru
    val carBrands = listOf(
        "Renault Scenic", "Renault Clio", "Renault Koleos",
        "Seat Leon", "Seat Alhambra", "Seat Arona",
        "VW Golf", "VW Arteon", "VW Tuareg"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(carBrands.first()) }

    // Pobieranie lokalizacji dla wybranej marki
    LaunchedEffect(selectedBrand) {
        isLoading = true
        errorMessage = null

        val normalizedBrand = selectedBrand.replace(" ", "")

        db.collection("CarsLocalization")
            .document(normalizedBrand)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fetchedLocations = doc.data?.mapNotNull { (_, value) ->
                        if (value is Map<*, *>) {
                            val lat = value["latitude"] as? Double
                            val lon = value["longitude"] as? Double
                            if (lat != null && lon != null) LatLng(lat, lon) else null
                        } else null
                    } ?: emptyList()

                    locations = fetchedLocations

                    if (fetchedLocations.isNotEmpty()) {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(fetchedLocations.first(), 8f)
                    }
                } else {
                    errorMessage = "Brak lokalizacji dla tej marki."
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("MapScreen", "Błąd pobierania lokalizacji", e)
                errorMessage = "Nie udało się pobrać lokalizacji."
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Górny pasek z menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(colorResource(id = R.color.grey2)),
            contentAlignment = Alignment.Center
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedBrand,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wybierz model", color = Color.White) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(220.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colorResource(id = R.color.blue),
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colorResource(id = R.color.grey2))
                ) {
                    carBrands.forEach { brand ->
                        DropdownMenuItem(
                            text = { Text(text = brand, color = Color.White) },
                            onClick = {
                                selectedBrand = brand
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

// Mapa z heatmapą
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (locations.isNotEmpty()) {

                    // KONFIGURACJA
                    val RADIUS_KM = 2.0
                    val MAX_INTENSITY = 50.0

                    // Stała dla szerokości geograficznej (PION - Latitude) 1 stopień szerokości to zawsze ok. 111 km.
                    val GRID_SIZE_LAT = RADIUS_KM / 111.0

                    // -GRUPOWANIE - TWORZENIE MAPY GĘSTOŚCI
                    val gridMap = mutableMapOf<Pair<Int, Int>, MutableList<LatLng>>()

                    for (point in locations) {
                        //  Grid X (Pion)
                        val gridX = (point.latitude / GRID_SIZE_LAT).toInt()

                        //  Grid Y (Poziom) - Z KOREKTĄ NA KRZYWIZNĘ
                        val latRad = Math.toRadians(point.latitude)
                        // Zabezpieczenie przed dzieleniem przez 0
                        val cosLat = kotlin.math.cos(latRad).let { if (it == 0.0) 0.0001 else it }

                        // Dynamiczny rozmiar siatki w poziomie bo iim wyżej na północ, tym szersze stopnie
                        val gridSizeLon = GRID_SIZE_LAT / cosLat
                        val gridY = (point.longitude / gridSizeLon).toInt()

                        val key = gridX to gridY
                        gridMap.getOrPut(key) { mutableListOf() }.add(point)
                    }

                    // Mapa: Klucz (X,Y) -> Ile jest aut
                    val densityMap = gridMap.mapValues { it.value.size }

                    // -== TWORZENIE Punktów DLA HEATMAPY i Odczyt Wag ---
                    val weightedLocations = densityMap.map { (key, count) ->

                        // Pobranie listy aut z danej kratki
                        val carsInGrid = gridMap[key]!!

                        //  punkt reprezentatywny
                        val representativePoint = carsInGrid.first()

                        // waga = liczebnosc grupy
                        val weight = count.toDouble()

                        WeightedLatLng(representativePoint, weight.coerceAtMost(MAX_INTENSITY))
                    }


                    //  KOLORÓWy I GRADIENT ---
                    val GREEN_RANGE_END = 5.0
                    val YELLOW_RANGE_START = 6.0
                    val YELLOW_RANGE_END = 10.0
                    val RED_RANGE_START = 11.0

                    val colors = intArrayOf(
                        android.graphics.Color.rgb(0, 255, 0),    // Zielony
                        android.graphics.Color.rgb(0, 255, 0),
                        android.graphics.Color.rgb(255, 255, 0),  // Żółty
                        android.graphics.Color.rgb(255, 255, 0),
                        android.graphics.Color.rgb(255, 0, 0),    // Czerwony
                        android.graphics.Color.rgb(255, 0, 0)
                    )

                    val startPoints = floatArrayOf(
                        (0.1 / MAX_INTENSITY).toFloat(),
                        (GREEN_RANGE_END / MAX_INTENSITY).toFloat(),
                        (YELLOW_RANGE_START / MAX_INTENSITY).toFloat(),
                        (YELLOW_RANGE_END / MAX_INTENSITY).toFloat(),
                        (RED_RANGE_START / MAX_INTENSITY).toFloat(),
                        1.0f
                    )

                    val gradient = Gradient(colors, startPoints)

                    //  RENDERowanie
                    val provider = HeatmapTileProvider.Builder()
                        .weightedData(weightedLocations)
                        .radius(50)
                        .gradient(gradient)
                        .maxIntensity(MAX_INTENSITY)
                        .opacity(0.7)
                        .build()

                    TileOverlay(
                        tileProvider = provider,
                        transparency = 0.3f
                    )
                }
            }


            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }


        // Dolny pasek
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colorResource(id = R.color.grey2))
        )
    }
}


