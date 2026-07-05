package com.example.habtrack.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.example.habtrack.data.ApiKeyStore
import com.example.habtrack.health.HealthConnectAvailability
import com.example.habtrack.health.HealthConnectManager
import com.example.habtrack.ui.theme.Obsidian

/**
 * Lets the user paste their own Anthropic API key, stored encrypted on-device.
 * The key is only used locally to call Claude for the AI Insights feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val apiKeyStore = remember { ApiKeyStore(context) }

    var apiKey by remember { mutableStateOf(apiKeyStore.getApiKey().orEmpty()) }
    var keyVisible by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val healthConnectManager = remember { HealthConnectManager(context) }
    var hcAvailability by remember { mutableStateOf<HealthConnectAvailability?>(null) }
    var hcPermissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        hcPermissionsGranted = granted.containsAll(healthConnectManager.getRequiredPermissions())
    }

    LaunchedEffect(Unit) {
        try {
            val availability = healthConnectManager.getAvailability()
            hcAvailability = availability
            if (availability is HealthConnectAvailability.Available) {
                val client = HealthConnectManager.getClient(context)
                if (client != null) {
                    hcPermissionsGranted = healthConnectManager.hasAllPermissions(client)
                }
            }
        } catch (_: Exception) {
            hcAvailability = HealthConnectAvailability.NotInstalled
        }
    }

    Scaffold(
        containerColor = Obsidian.Bg,
        topBar = {
            TopAppBar(
                title = { Text("settings", style = MaterialTheme.typography.headlineMedium, color = Obsidian.TextHi) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Obsidian.TextMid)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Obsidian.Bg,
                    titleContentColor = Obsidian.TextHi
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Obsidian.Bg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionLabel("Anthropic API key")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Needed for the AI Insights feature on the Analytics tab. " +
                    "Your key is stored encrypted on this device only and is never sent anywhere except directly to Anthropic.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Obsidian.TextLow
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    savedMessage = null
                },
                label = { Text("API key") },
                placeholder = { Text("sk-ant-...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key",
                            tint = Obsidian.TextLow
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        apiKeyStore.saveApiKey(apiKey)
                        savedMessage = "API key saved."
                    },
                    enabled = apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Obsidian.Accent,
                        contentColor = Obsidian.Bg,
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContentColor = Obsidian.TextLow
                    )
                ) {
                    Text("SAVE", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        apiKeyStore.clearApiKey()
                        apiKey = ""
                        savedMessage = "API key removed."
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Obsidian.Stroke),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Obsidian.TextMid)
                ) {
                    Text("CLEAR", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            savedMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Obsidian.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Obsidian.StrokeSoft)
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Health Connect")
                val dotColor = when (hcAvailability) {
                    is HealthConnectAvailability.Available -> if (hcPermissionsGranted) Obsidian.Accent else Color(0xFFF2B558)
                    is HealthConnectAvailability.NotInstalled, is HealthConnectAvailability.UpdateRequired -> Color(0xFFF2B5B5)
                    null -> Obsidian.TextLow
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Lets HabTrack auto-sync steps, active calories, and distance into any habit " +
                    "you opt in via that habit's settings.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Obsidian.TextLow
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val availability = hcAvailability) {
                null -> {
                    Text("Checking availability...", fontSize = 13.sp, color = Obsidian.TextLow)
                }
                is HealthConnectAvailability.NotInstalled -> {
                    Text(
                        "Health Connect is not installed on this device.",
                        fontSize = 13.sp,
                        color = Color(0xFFF2B5B5)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val playStorePackage = "com.google.android.apps.healthdata"
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$playStorePackage"))
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$playStorePackage")
                            )
                            runCatching { context.startActivity(marketIntent) }
                                .onFailure { context.startActivity(webIntent) }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
                    ) {
                        Text("INSTALL FROM PLAY STORE", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                is HealthConnectAvailability.UpdateRequired -> {
                    Text(
                        "Health Connect needs to be updated before it can be used.",
                        fontSize = 13.sp,
                        color = Color(0xFFF2B5B5)
                    )
                }
                is HealthConnectAvailability.Available -> {
                    if (hcPermissionsGranted) {
                        Text("Connected", color = Obsidian.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Not connected yet.", fontSize = 13.sp, color = Obsidian.TextLow)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(healthConnectManager.getRequiredPermissions()) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
                        ) {
                            Text("CONNECT HEALTH CONNECT", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
