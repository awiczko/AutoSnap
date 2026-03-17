package com.example.autosnap_inz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object BitmapUtils {

    /**
     * konwertowanioe bitmapy na tablice
     */
    suspend fun bitmapToByteArray(bitmap: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }

    /**
     * konwertowanie tablicy na bitmape
     */
    suspend fun byteArrayToBitmap(byteArray: ByteArray): Bitmap = withContext(Dispatchers.Default) {
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
