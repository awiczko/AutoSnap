package com.example.autosnap_inz

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.autosnap_inz.NotificationReceiver
import com.google.firebase.auth.FirebaseAuth


import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.Identity
import androidx.activity.compose.LocalActivity


@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val activity = LocalActivity.current

    var isNotificationEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("notificationsSwitchState", false))
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scheduleNotifications(context, alarmManager)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1F1C1C))
            .padding(25.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        SettingsSection(title = "Konto") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Zmień użytkownika",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        Log.d("SettingsScreen", "Kliknięto Wyloguj")
//                        onLogout()

                        activity?.let { act ->
                            val auth = FirebaseAuth.getInstance()
                            val oneTapClient = Identity.getSignInClient(act)

                            oneTapClient.signOut().addOnCompleteListener {
                                auth.signOut()
                                Log.d("SettingsScreen", "Wylogowano z Firebase i One Tap")

                                // Przejście do LogInActivity
                                act.startActivity(Intent(act, LogInActivity::class.java))
                                act.finish()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FA))
                ) {
                    Text("Wyloguj się", color = Color.White)
                }
            }
        }

        SettingsSection(title = "Powiadomienia") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Powiadomienia włączone", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = isNotificationEnabled,
                    onCheckedChange = { checked ->
                        isNotificationEnabled = checked
                        with(sharedPreferences.edit()) {
                            putBoolean("notificationsSwitchState", checked)
                            apply()
                        }

                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    scheduleNotifications(context, alarmManager)
                                }
                            } else {
                                scheduleNotifications(context, alarmManager)
                            }
                        } else {
                            cancelNotifications(context, alarmManager)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0075FA),
                        checkedTrackColor = Color(0x330075FA),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        SettingsSection(title = "Informacje") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wersja aplikacji", color = Color.White, modifier = Modifier.weight(1f))
                Text("1.0.0", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF64B5F6),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }

    Spacer(modifier = Modifier.height(16.dp))
}

fun scheduleNotifications(context: Context, alarmManager: AlarmManager) {
    val intent = Intent(context, NotificationReceiver::class.java)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    else
        PendingIntent.FLAG_UPDATE_CURRENT

    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
    val triggerAtMillis = System.currentTimeMillis() + 10_000L

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            alarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(alarmIntent)
            return
        }
    }

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    } catch (e: SecurityException) {
        Log.e("SettingsScreen", "Brak pozwolenia na Exact Alarm", e)
    }
}



fun cancelNotifications(context: Context, alarmManager: AlarmManager) {
    val intent = Intent(context, NotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    alarmManager.cancel(pendingIntent)
}


