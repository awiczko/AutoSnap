package com.example.autosnap_inz

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

// Klasa danych dla osiągnięcia
data class Achievement(
    val title: String,
    val description: String,
    val image: Bitmap
)

@Composable
fun AchievementItemCompose(title: String, description: String, bitmap: Bitmap) { // ()title: String, description: String, bitmap: Bitmap
    val imageBitmap = bitmap.asImageBitmap()   // tutaj byl bitmap.asImageBitmap

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.grey2)))
    {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp))
        {
            // Ikona osiągnięcia
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(color = Color.White, shape = CircleShape)
                    .border(width = 2.dp, color = Color.Black, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center)
            {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Tekst osiągnięcia
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.hum777k)),
                    color = colorResource(id = R.color.blue),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}