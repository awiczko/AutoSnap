import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.autosnap_inz.AchievementsScreen
import com.example.autosnap_inz.CarsLibraryCompose
import com.example.autosnap_inz.ProfileScreen
import com.example.autosnap_inz.R
import com.example.autosnap_inz.SettingsScreen
import com.example.autosnap_inz.SqlLiteHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.google.android.gms.location.Priority
import java.io.File

@Composable
fun CustomBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "achievements" to R.drawable.badge,
        "collection" to R.drawable.gallery,
        "camera" to R.drawable.shutter_camera,
        "profile" to R.drawable.user,
        "settings" to R.drawable.settings
    )

    Surface(
        shadowElevation = 8.dp,
        color = colorResource(R.color.blue),
        shape = RoundedCornerShape(
            topStart = 15.dp,
            topEnd = 15.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (name, iconRes) ->
                if (name == "camera") {
                    FloatingActionButton(
                        onClick = onCameraClick,
                        containerColor = Color.White,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = name,
                            tint = Color.Black,
                            modifier = Modifier
                                .size(64.dp)
                        )
                    }
                } else {
                    val isSelected = currentTab == name
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) colorResource(R.color.blue) else Color.White, // kolor obrysu
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                shape = CircleShape
                            )
                            .background(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onTabSelected(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = name,
                            tint = if (isSelected) colorResource(R.color.blue) else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MenuScreenWithCustomBottomBar() {
    var currentTab by remember { mutableStateOf("achievements") }
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val uniqueUserID = currentUser?.uid ?: "test_user"

    val carModels = remember { mutableStateListOf<String>() }
    val carBitmaps = remember { mutableStateListOf<Bitmap>() }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // -- launcher do lokalizacji -
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
        }
    }

    // -- wczytanie kolekcji po zmianie zakaladki
    LaunchedEffect(currentTab) {
        if (currentTab == "collection") {
            carModels.clear()
            carBitmaps.clear()

            val db = SqlLiteHelper(context)
            val cursor = db.readUserData(uniqueUserID)

            cursor?.use {
                while (it.moveToNext()) {
                    val model = it.getString(2)
                    val imageBytes = it.getBlob(3)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    carModels.add(model)
                    carBitmaps.add(bitmap)
                }
            } ?: run {
                Toast.makeText(context, "Brak zdjęć w kolekcji", Toast.LENGTH_SHORT).show()
            }
        }
    }


//  Zmienna do przechowywania sciezki do zdjecia
    var currentPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }


// Robienie zdjęcia
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture() // Zmiana kontraktu
    ) { success ->
        // Sprawdzamy czy się udało I czy mamy URI gdzie zapisano plik
        if (success && currentPhotoUri != null) {
            try {
                // 1. Wczytujemy zdjęcie
                val rawBitmap = getOptimizedBitmap(context, currentPhotoUri!!)

                if (rawBitmap != null) {
                    // 2. Przygotowujanie bitmapę
                    val aiInputBitmap = resizeWithPadding(rawBitmap, 320)

                    // Użycie bitmapy
                    val bitmap = aiInputBitmap

                    val recognizedModel = recognizeCarModel(context, bitmap) ?: "Unknown Model"

                    // Zapis do lokalnej bazy
                    val db = SqlLiteHelper(context)
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, stream)
                    val byteArray = stream.toByteArray()
                    db.insertData(uniqueUserID, recognizedModel, byteArray)

                    carModels.add(recognizedModel)
                    carBitmaps.add(bitmap)

                    // Lokalizacja
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        )
                            .addOnSuccessListener { location ->
                                sendCarLocationToFirestore(
                                    userID = uniqueUserID,
                                    carName = recognizedModel.replace(" ", ""),
                                    latitude = location?.latitude,
                                    longitude = location?.longitude
                                )
                            }
                            .addOnFailureListener {
                                Log.e("Firebase", "błąd lokalizacji!", it)
                                sendCarLocationToFirestore(
                                    userID = uniqueUserID,
                                    carName = recognizedModel.replace(" ", ""),
                                    latitude = null,
                                    longitude = null
                                )
                            }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    // Logika punktów
                    if (recognizedModel == "Unknown Model") {
                        addPointsToUser(uniqueUserID, 0L)
                        Toast.makeText(context, "Nierozpoznano auta", Toast.LENGTH_SHORT).show()
                    } else {
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val modelDocId = recognizedModel.replace(" ", "")

                        firestore.collection("AutoSnapInz")
                            .document("Cars")
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val pointsFromDb = document.getLong(modelDocId) ?: 0L
                                    addPointsToUser(uniqueUserID, pointsFromDb)
                                    Toast.makeText(context, "Model: $recognizedModel (+${pointsFromDb} pkt)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.w("Firestore", "Brak modelu w bazie: $recognizedModel")
                                    addPointsToUser(uniqueUserID, 5L)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Błąd pobierania punktów", e)
                                addPointsToUser(uniqueUserID, 5L)
                            }
                    }
                    // Sukces!
                    // Toast.makeText(context, "Zrobiono zdjęcie: $recognizedModel", Toast.LENGTH_SHORT).show() // Opcjonalny toast
                } else {
                    Toast.makeText(context, "Błąd przetwarzania zdjęcia", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Wystąpił błąd podczas zapisu", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Anulowano robienie zdjęcia
            // Toast.makeText(context, "Anulowano", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Uruchomienie aparatu -
    var startCamera by remember { mutableStateOf(false) }

    if (startCamera) {
        CameraPermissionLauncher {
            // tworzenie pliku
            val file = createImageFile(context)

            // 2. Pobieramy uri
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.autosnap_inz.provider",
                file
            )

            // 3. URI w zmiennej stanu
            currentPhotoUri = uri

            // 4. Uruchomiony aparat
            cameraLauncher.launch(uri)

            // Resetet flagi
            startCamera = false
        }
    }

    // --- UI -
    Scaffold(
        containerColor = colorResource(R.color.grey),
        bottomBar = {
            CustomBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onCameraClick = { startCamera = true },
                modifier = Modifier.zIndex(3f)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                "achievements" -> AchievementsScreen()
                "collection" -> CarsLibraryCompose(
                    models = carModels,
                    bitmaps = carBitmaps,
                    onDelete = { model, index ->
                        if (index in carModels.indices && index in carBitmaps.indices) {
                            carModels.removeAt(index)
                            carBitmaps.removeAt(index)
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            val db = SqlLiteHelper(context)
                            db.deleteCarByUserAndModel(uniqueUserID, model)
                        }
                    })
                "profile" -> ProfileScreen()
                "settings" -> SettingsScreen(onLogout = {})
            }
        }
    }
}



@Composable
fun CameraPermissionLauncher(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (!granted) {
                Toast.makeText(context, "Brak uprawnienia do aparatu", Toast.LENGTH_SHORT).show()
            } else {
                onPermissionGranted()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        } else {
            onPermissionGranted()
        }
    }
}

fun addPointsToUser(uid: String, pointsToAdd: Long) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("AutoSnapInz")
        .document("UserScore")
        .collection("test")
        .document(uid)

    userRef.update("points", FieldValue.increment(pointsToAdd))
        .addOnSuccessListener {
            Log.d("FirebasePoints", "Punkty zaktualizowane: +$pointsToAdd")
        }
        .addOnFailureListener { e ->
            Log.e("FirebasePoints", "Błąd aktualizacji punktów", e)
            //  jeśli dokument nie istnieje, to go utworzy
            userRef.set(mapOf("points" to pointsToAdd), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FirebasePoints", "Utworzono nowy dokument z punktami: $pointsToAdd")
                }
                .addOnFailureListener { e2 ->
                    Log.e("FirebasePoints", "Błąd tworzenia dokumentu", e2)
                }
        }
}

fun sendCarLocationToFirestore(
    userID: String,
    carName: String,
    latitude: Double?,
    longitude: Double?
) {
    val db = FirebaseFirestore.getInstance()

    val locationData = mapOf(
        "${userID}_${System.currentTimeMillis()}" to mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
    )

    db.collection("CarsLocalization")
        .document(carName)   // dokument = model auta
        .set(locationData, SetOptions.merge()) // merge = dodaje nowe pole bez nadpisywania istniejących
        .addOnSuccessListener {
            Log.d("Firestore", "Dodano lokalizację dla $carName")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Błąd zapisu lokalizacji", e)
        }

}



// Prawdopodobnie funkcja testowa 2
fun recognizeCarModel(context: Context, bitmap: Bitmap): String {
    try {
        //  ROZPOZNAWANIE MARKI ===
        val brandTflite = Interpreter(loadModelFile(context, "model/brand_classifier_vgg16.tflite"))
        val brandLabels = context.assets.open("labels/brands.txt").bufferedReader().readLines()

        val inputSize = 224 // tutaj zmienic dane domyslnie 224
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(scaledBitmap)

        val brandOutput = Array(1) { FloatArray(brandLabels.size) }
        brandTflite.run(inputBuffer, brandOutput)
        brandTflite.close()

        //  Znajdź najlepszy wynik marki
        val brandIndex = brandOutput[0].indices.maxByOrNull { brandOutput[0][it] } ?: -1 // wartownik, sprawdzenie bledu czy indeks tablicy ok
        val recognizedBrand = if (brandIndex != -1) brandLabels[brandIndex] else "Unknown Brand"

        Log.d("AutoSnap pred", "Rozpoznana marka: $recognizedBrand")

        // =wybor modelu dla marki ===
        val brandFileBase = recognizedBrand.lowercase().trim()
        val availableModels = context.assets.list("models") ?: emptyArray()
        val availableLabels = context.assets.list("labels") ?: emptyArray()

        val modelFileName = "models/${brandFileBase}_models.tflite"
        val labelFileName = "labels/${brandFileBase}_models.txt"

        if (!availableModels.contains("${brandFileBase}_models.tflite") ||
            !availableLabels.contains("${brandFileBase}_models.txt")
        ) {
            Log.e("AutoSnap pred", "Brak modelu lub etykiet dla marki: $recognizedBrand")
            return recognizedBrand // Zwracamy samą markę
        }

        // = ROZPOZNAWANIE MODELU DLA MARKI ===
        val modelTflite = Interpreter(loadModelFile(context, modelFileName))
        val modelLabels = context.assets.open(labelFileName).bufferedReader().readLines()

        val modelOutput = Array(1) { FloatArray(modelLabels.size) }
        modelTflite.run(inputBuffer, modelOutput)
        modelTflite.close()

        // znajdowanie najlepszego wyniku dla mmodelu
        val modelIndex = modelOutput[0].indices.maxByOrNull { modelOutput[0][it] } ?: -1
        val recognizedModel = if (modelIndex != -1) modelLabels[modelIndex] else "Unknown Model"

        Log.d("AutoSnap pred", "Rozpoznany model: $recognizedModel")

        // pelne rozpoznanie
        return "$recognizedBrand $recognizedModel"

    } catch (e: Exception) {
        Log.e("AutoSnap pred", "Błąd rozpoznawania: ${e.message}", e)
        return "Unknown Model"
    }
}



@Composable
fun RequestLocationPermission(onGranted: () -> Unit) {
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onGranted()
            } else {
                Toast.makeText(context, "Brak uprawnienia do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            onGranted()
        }
    }
}

private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelName)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val inputSize = 224 // domyslnie 224
    val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // float32
    byteBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(inputSize * inputSize)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    var pixel = 0
    for (i in 0 until inputSize) {
        for (j in 0 until inputSize) {
            val value = intValues[pixel++]
            val r = ((value shr 16) and 0xFF).toFloat()
            val g = ((value shr 8) and 0xFF).toFloat()
            val b = (value and 0xFF).toFloat()
            // ResNet50 preprocessing: [-1,1]
            byteBuffer.putFloat(r / 127.5f - 1f)
            byteBuffer.putFloat(g / 127.5f - 1f)
            byteBuffer.putFloat(b / 127.5f - 1f)
        }
    }
    byteBuffer.rewind()
    return byteBuffer
}


fun getOptimizedBitmap(context: Context, photoUri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }


        contentResolver.openInputStream(photoUri)?.use { BitmapFactory.decodeStream(it, null, options) }


        var scale = 1
        while (options.outWidth / scale / 2 >= 400 && options.outHeight / scale / 2 >= 400) {
            scale *= 2
        }

        val loadOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        contentResolver.openInputStream(photoUri)?.use { BitmapFactory.decodeStream(it, null, loadOptions) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Skalowanie z czarnymi pasami
fun resizeWithPadding(source: Bitmap, targetSize: Int): Bitmap {
    val width = source.width
    val height = source.height
    val scale = if (width > height) targetSize.toFloat() / width else targetSize.toFloat() / height

    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    val scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight, true)

    val finalBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val left = (targetSize - newWidth) / 2f
    val top = (targetSize - newHeight) / 2f
    canvas.drawBitmap(scaledBitmap, left, top, null)

    return finalBitmap
}

// 3. Tworzenie pliku tymczasowego
fun createImageFile(context: Context): File {
    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.cacheDir)
}