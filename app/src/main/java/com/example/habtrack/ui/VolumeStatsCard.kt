package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.utils.VolumeStatsCalculator

@Composable
fun VolumeStatsCard(volumeStats: HabitVolumeStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with habit name
            Text(
                text = volumeStats.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            // Volume statistics grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatHighlight(
                    label = "Personal Record",
                    value = VolumeStatsCalculator.formatVolume(volumeStats.personalRecord, volumeStats.unit),
                    backgroundColor = Color(0xFFFFF3E0),
                    modifier = Modifier.weight(1f)
                )
                StatHighlight(
                    label = "Daily Average",
                    value = VolumeStatsCalculator.formatVolume(volumeStats.averageDailyVolume, volumeStats.unit),
                    backgroundColor = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VolumeStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        )
    }
}

@Composable
fun StatHighlight(
    label: String,
    value: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun VolumeStatsGrid(volumeStatsList: List<HabitVolumeStats>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📊 Volume Statistics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        if (volumeStatsList.isEmpty()) {
            Text(
                text = "No habits tracked yet",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            volumeStatsList.forEach { stats ->
                VolumeStatsCard(stats)
            }
        }
    }
}
