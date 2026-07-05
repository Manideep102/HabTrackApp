package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.ui.theme.Obsidian
import com.example.habtrack.utils.VolumeStatsCalculator

@Composable
fun VolumeStatsCard(volumeStats: HabitVolumeStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with habit name
        Text(
            text = volumeStats.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextHi
        )

        // Volume statistics grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VolumeStatItem(
                label = "Today",
                value = VolumeStatsCalculator.formatVolume(volumeStats.todayVolume, volumeStats.unit),
                modifier = Modifier.weight(1f)
            )
            VolumeStatItem(
                label = "Weekly",
                value = VolumeStatsCalculator.formatVolume(volumeStats.weeklyVolume, volumeStats.unit),
                modifier = Modifier.weight(1f)
            )
            VolumeStatItem(
                label = "Monthly",
                value = VolumeStatsCalculator.formatVolume(volumeStats.monthlyVolume, volumeStats.unit),
                modifier = Modifier.weight(1f)
            )
            VolumeStatItem(
                label = "Yearly",
                value = VolumeStatsCalculator.formatVolume(volumeStats.yearlyVolume, volumeStats.unit),
                modifier = Modifier.weight(1f)
            )
        }

        // Personal record and average stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatHighlight(
                label = "Personal record",
                value = VolumeStatsCalculator.formatVolume(volumeStats.personalRecord, volumeStats.unit),
                accent = true,
                modifier = Modifier.weight(1f)
            )
            StatHighlight(
                label = "Daily average",
                value = VolumeStatsCalculator.formatVolume(volumeStats.averageDailyVolume, volumeStats.unit),
                accent = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VolumeStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Obsidian.TextHi
        )
        Text(
            text = label.uppercase(),
            fontSize = 8.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextLow
        )
    }
}

@Composable
fun StatHighlight(
    label: String,
    value: String,
    accent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) Obsidian.AccentDim else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (accent) Obsidian.AccentBorder else Obsidian.StrokeSoft,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) Obsidian.Accent else Obsidian.TextHi
        )
        Text(
            text = label.uppercase(),
            fontSize = 8.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextLow
        )
    }
}

@Composable
fun VolumeStatsGrid(volumeStatsList: List<HabitVolumeStats>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Volume statistics")

        if (volumeStatsList.isEmpty()) {
            Text(
                text = "No habits tracked yet",
                fontSize = 14.sp,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            volumeStatsList.forEach { stats ->
                VolumeStatsCard(stats)
            }
        }
    }
}
