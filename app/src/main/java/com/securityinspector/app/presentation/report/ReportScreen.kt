package com.securityinspector.app.presentation.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securityinspector.app.domain.model.EvidenceType
import com.securityinspector.app.presentation.components.DeviceInfoCard
import com.securityinspector.app.presentation.components.RiskBadge
import com.securityinspector.app.presentation.components.ScoreGauge
import com.securityinspector.app.presentation.components.SecurityFindingCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FindingFilter { ALL, INFORMATIONAL, HEURISTIC, FLAGGED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    scanId: Long,
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FindingFilter.ALL) }

    LaunchedEffect(scanId) { viewModel.loadScan(scanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.scanResult == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Report not found") }
            }

            else -> {
                val scan = uiState.scanResult!!
                val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

                val filteredFindings = scan.findings.filter { finding ->
                    val matchesSearch = searchQuery.isBlank() ||
                        finding.title.contains(searchQuery, ignoreCase = true) ||
                        finding.description.contains(searchQuery, ignoreCase = true) ||
                        finding.category.displayName.contains(searchQuery, ignoreCase = true)

                    val matchesFilter = when (filter) {
                        FindingFilter.ALL -> true
                        FindingFilter.INFORMATIONAL -> finding.evidenceType == EvidenceType.INFORMATIONAL
                        FindingFilter.HEURISTIC -> finding.evidenceType == EvidenceType.HEURISTIC
                        FindingFilter.FLAGGED -> !finding.isPositiveFinding
                    }
                    matchesSearch && matchesFilter
                }.sortedByDescending { it.severity.weight }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ScoreGauge(score = scan.securityScore, riskLevel = scan.riskLevel, size = 140.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                RiskBadge(riskLevel = scan.riskLevel)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    dateFormat.format(Date(scan.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Export Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        ExportFormat.PDF to "PDF",
                                        ExportFormat.JSON to "JSON",
                                        ExportFormat.CSV to "CSV"
                                    ).forEach { (format, label) ->
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.export(format) { intent ->
                                                    context.startActivity(intent)
                                                }
                                            },
                                            enabled = !uiState.isExporting
                                        ) {
                                            Icon(
                                                Icons.Filled.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.height(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(label)
                                        }
                                    }
                                }
                                uiState.exportError?.let {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    item {
                        DeviceInfoCard(
                            deviceInfo = scan.deviceInfo,
                            lastScanLabel = dateFormat.format(Date(scan.timestamp))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search findings…") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true
                        )
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(FindingFilter.entries) { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }

                    items(filteredFindings) { finding ->
                        SecurityFindingCard(finding = finding)
                    }

                    if (filteredFindings.isEmpty()) {
                        item {
                            Text(
                                "No findings match your search/filter.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
