package com.example.autosnap_inz

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF1F1C1C)) // grey // moze byc blad z importem funkcji
            .padding(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.hum777k)),
                modifier = Modifier
                    .width(375.dp)
                    .height(175.dp)
                    .background(
                        color = colorResource(id = R.color.blue),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(100.dp))

            // Google Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = colorResource(id = R.color.blue),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(
                            width = 2.dp,
                            color = colorResource(id = R.color.blue),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                Image(
                    painter = painterResource(R.drawable.google),
                    contentDescription = stringResource(R.string.google_icon_desc),
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Sign in Button
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .background(
                        color = colorResource(id = R.color.blue),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onGoogleSignInClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.continuewithgoogle),
                    contentDescription = stringResource(R.string.google_icon_desc),
                    modifier = Modifier
                        .width(185.dp)
                        .height(50.dp)
                )
            }
        }

        // versja
        Text(
            text = stringResource(R.string.version),
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}