package com.securityinspector.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securityinspector.app.domain.model.DeviceInfo
import java.text.DecimalFormat

@Composable
fun DeviceInfoCard(deviceInfo: DeviceInfo, lastScanLabel: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Device", "${deviceInfo.manufacturer} ${deviceInfo.model}")
            InfoRow("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})")
            InfoRow("Security Patch", deviceInfo.securityPatchLevel)
            InfoRow("CPU Architecture", deviceInfo.cpuArchitecture)
            InfoRow("Last Scan", lastScanLabel)

            Spacer(modifier = Modifier.height(14.dp))

            val storageUsedFraction = 1f - (deviceInfo.availableStorageBytes.toFloat() /
                deviceInfo.totalStorageBytes.toFloat()).coerceIn(0f, 1f)
            UsageBar(
                label = "Storage",
                usedLabel = "${formatBytes(deviceInfo.totalStorageBytes - deviceInfo.availableStorageBytes)} " +
                    "/ ${formatBytes(deviceInfo.totalStorageBytes)}",
                fraction = storageUsedFraction
            )

            Spacer(modifier = Modifier.height(10.dp))

            val ramUsedFraction = 1f - (deviceInfo.availableRamBytes.toFloat() /
                deviceInfo.totalRamBytes.toFloat()).coerceIn(0f, 1f)
            UsageBar(
                label = "RAM",
                usedLabel = "${formatBytes(deviceInfo.totalRamBytes - deviceInfo.availableRamBytes)} " +
                    "/ ${formatBytes(deviceInfo.totalRamBytes)}",
                fraction = ramUsedFraction
            )

            Spacer(modifier = Modifier.height(10.dp))

            UsageBar(
                label = "Battery${if (deviceInfo.isCharging) " (charging)" else ""}",
                usedLabel = "${deviceInfo.batteryPercent}%",
                fraction = deviceInfo.batteryPercent / 100f
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UsageBar(label: String, usedLabel: String, fraction: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = usedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val safeIndex = digitGroups.coerceIn(0, units.size - 1)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, safeIndex.toDouble())) +
        " " + units[safeIndex]
}
