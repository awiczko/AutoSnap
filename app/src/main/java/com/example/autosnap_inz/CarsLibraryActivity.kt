package com.example.autosnap_inz

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.room.RoomMasterTable.TABLE_NAME
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CarsLibraryActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    // Observable lists dla Compose
    private val carModels = mutableStateListOf<String>()
    private val carBitmaps = mutableStateListOf<Bitmap>()

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val uniqueUserID = currentUser?.uid ?: "test_user"

        // Wczytywanie danych z bazy w tle
        CoroutineScope(Dispatchers.IO).launch {
            val db = SqlLiteHelper(this@CarsLibraryActivity)
            val cursor = db.readUserData(uniqueUserID)

            cursor?.use {
                while (it.moveToNext()) {
                    val model = it.getString(2)
                    val imageBytes = it.getBlob(3)

                    try {
                        val bitmap = BitmapUtils.byteArrayToBitmap(imageBytes)
                        carModels.add(model)
                        carBitmaps.add(bitmap)
                        Log.d("CarsLibrary", "Odczytano model=$model dla user=$uniqueUserID")
                    } catch (e: Exception) {
                        Log.e("CarsLibrary", "Błąd dekodowania obrazu dla model=$model", e)
                    }
                }
            } ?: run {
                Log.d("CarsLibrary", "Brak danych w DB dla user=$uniqueUserID")
            }
        }

        setContent {
            CarsLibraryCompose(
                models = carModels,
                bitmaps = carBitmaps,
                onDelete = { model, index ->
                    deleteCar(model, index)
                }
            )
        }
    }




    fun addCar(model: String, bitmap: Bitmap) {
        val currentUser = auth.currentUser
        val uniqueUserID = currentUser?.uid ?: "test_user"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val byteArray = BitmapUtils.bitmapToByteArray(bitmap)
                val db = SqlLiteHelper(this@CarsLibraryActivity)
                db.insertData(uniqueUserID, model, byteArray)

                // Odświeżenie list
                launch(Dispatchers.Main) {
                    carModels.add(model)
                    carBitmaps.add(bitmap)
                    Log.d(
                        "CarsLibrary",
                        "Dodano nowe auto: model=$model, userId=$uniqueUserID, imgSize=${byteArray.size}"
                    )
                }
            } catch (e: Exception) {
                Log.e("CarsLibrary", "Błąd zapisu auta do DB", e)
            }
        }
    }

    // Gettery dla Compose
    fun getCarModels() = carModels
    fun getCarBitmaps() = carBitmaps

    fun deleteCar(model: String, index: Int) {
        val currentUser = auth.currentUser
        val uniqueUserID = currentUser?.uid ?: "test_user"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SqlLiteHelper(this@CarsLibraryActivity)

                // Usuwanie na podstawie userId i modelu
                db.deleteCarByUserAndModel(uniqueUserID, model)

                // Odświeżenie list
                launch(Dispatchers.Main) {
                    if (index in carModels.indices && index in carBitmaps.indices) {
                        carModels.removeAt(index)
                        carBitmaps.removeAt(index)
                        Log.d("CarsLibrary", "Usunięto auto: model=$model, userId=$uniqueUserID")
                    }
                }
            } catch (e: Exception) {
                Log.e("CarsLibrary", "Błąd usuwania auta z DB", e)
            }
        }
    }
}
