package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.health.HealthMetric
import com.example.habtrack.ui.theme.Obsidian

/**
 * Full-screen New Habit flow (Obsidian design 1d): name, Health Connect
 * source chips, +/- goal stepper, and a CREATE HABIT CTA.
 */
@Composable
fun AddHabitScreen(
    healthConnectAvailable: Boolean,
    onBack: () -> Unit,
    onSave: (name: String, goal: Float, unit: String, increment: Float, metric: HealthMetric?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMetric by remember { mutableStateOf<HealthMetric?>(null) } // null = manual
    var goal by remember { mutableFloatStateOf(30f) }
    var unit by remember { mutableStateOf("min") }
    var increment by remember { mutableStateOf("1") }

    // Step size for the +/- goal stepper, per source
    val step = when (selectedMetric) {
        HealthMetric.STEPS -> 500f
        HealthMetric.ACTIVE_CALORIES -> 50f
        HealthMetric.DISTANCE -> 0.5f
        null -> 5f
    }

    fun selectMetric(metric: HealthMetric?) {
        selectedMetric = metric
        when (metric) {
            HealthMetric.STEPS -> { unit = "steps"; goal = 5000f }
            HealthMetric.ACTIVE_CALORIES -> { unit = "kcal"; goal = 400f }
            HealthMetric.DISTANCE -> { unit = "km"; goal = 3f }
            null -> { unit = "min"; goal = 30f }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.Bg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Obsidian.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, Obsidian.Stroke, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Obsidian.TextMid,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("New habit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
            }

            // ── name ──
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                "NAME",
                style = MaterialTheme.typography.labelSmall,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Morning run", color = Obsidian.TextFaint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // ── source chips ──
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "TRACK FROM",
                style = MaterialTheme.typography.labelSmall,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceChip("Manual", selectedMetric == null) { selectMetric(null) }
                SourceChip(
                    "Steps",
                    selectedMetric == HealthMetric.STEPS,
                    enabled = healthConnectAvailable
                ) { selectMetric(HealthMetric.STEPS) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceChip(
                    "Active calories",
                    selectedMetric == HealthMetric.ACTIVE_CALORIES,
                    enabled = healthConnectAvailable
                ) { selectMetric(HealthMetric.ACTIVE_CALORIES) }
                SourceChip(
                    "Distance",
                    selectedMetric == HealthMetric.DISTANCE,
                    enabled = healthConnectAvailable
                ) { selectMetric(HealthMetric.DISTANCE) }
            }
            if (!healthConnectAvailable) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Connect Health Connect in Settings to unlock automatic tracking.",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Obsidian.TextFaint,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // ── goal stepper ──
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "DAILY GOAL",
                style = MaterialTheme.typography.labelSmall,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Obsidian.Surface)
                    .border(1.dp, Obsidian.Stroke, RoundedCornerShape(20.dp))
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepperButton("−", accent = false) {
                    goal = (goal - step).coerceAtLeast(step)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatValue(goal),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Obsidian.TextHi,
                            lineHeight = 38.sp
                        )
                        Text(
                            " $unit",
                            fontSize = 14.sp,
                            color = Obsidian.TextLow,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "PER DAY",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Medium,
                        color = Obsidian.TextLow
                    )
                }
                StepperButton("+", accent = true) { goal += step }
            }

            // ── manual-only fields ──
            if (selectedMetric == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = increment,
                        onValueChange = { increment = it },
                        label = { Text("Increment") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (selectedMetric != null)
                    "Progress updates automatically from Health Connect. You can still log manually anytime."
                else
                    "Tap the habit each day to log progress toward your goal.",
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = Obsidian.TextFaint,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── CTA ──
        Box(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        goal,
                        unit.trim().ifEmpty { "times" },
                        increment.toFloatOrNull() ?: 1f,
                        selectedMetric
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Obsidian.Accent,
                    contentColor = Obsidian.Bg,
                    disabledContainerColor = Color.White.copy(alpha = 0.06f),
                    disabledContentColor = Obsidian.TextLow
                )
            ) {
                Text("CREATE HABIT", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Obsidian.AccentDim else Color.Transparent)
            .border(
                1.dp,
                if (selected) Obsidian.Accent else Obsidian.Stroke,
                RoundedCornerShape(99.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = when {
                selected -> Obsidian.Accent
                !enabled -> Obsidian.TextFaint
                else -> Obsidian.TextMid
            }
        )
    }
}

@Composable
private fun StepperButton(symbol: String, accent: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (accent) Obsidian.AccentDim else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (accent) Obsidian.Accent else Obsidian.Stroke,
                RoundedCornerShape(15.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            fontSize = 20.sp,
            color = if (accent) Obsidian.Accent else Obsidian.TextMid
        )
    }
}
