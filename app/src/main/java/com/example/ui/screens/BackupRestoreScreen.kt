package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PaymentPaid
import com.example.ui.viewmodel.RepairViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: RepairViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var exportedJson by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    fun shareBackup(jsonText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, jsonText)
            type = "application/json"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share UDM Backup JSON"))
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UDM Backup", text)
        clipboard.setPrimaryClip(clip)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Backup copied to clipboard!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // EXPORT BACKUP
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BACKUP DATA (OFFLINE JSON)",
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Exports all repairs, customers, payment histories, prices, brands, and settings into structured JSON. No Google Drive required.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.exportBackup { json ->
                                    exportedJson = json
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Backup created successfully!")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("generate_backup_btn")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF090E1A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERATE BACKUP", color = Color(0xFF090E1A), fontWeight = FontWeight.Bold)
                        }

                        if (exportedJson.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { copyToClipboard(exportedJson) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy JSON", color = CyanAccent)
                                }
                                OutlinedButton(
                                    onClick = { shareBackup(exportedJson) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share / Save", color = CyanAccent)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = exportedJson.take(500) + if (exportedJson.length > 500) "\n... [Full JSON Exported]" else "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Backup Preview") },
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // RESTORE BACKUP
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RESTORE FROM BACKUP",
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Paste a previously exported JSON backup here to restore all data, job numbers, and payment history.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = importJsonInput,
                            onValueChange = { importJsonInput = it },
                            label = { Text("Paste JSON Backup String") },
                            minLines = 4,
                            placeholder = { Text("{\n  \"app\": \"UDM MOBILE REPAIR\",\n  \"repairs\": [...]\n}") },
                            modifier = Modifier.fillMaxWidth().testTag("restore_json_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (importJsonInput.isNotBlank()) {
                                    showRestoreConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PaymentPaid),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restore_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESTORE DATABASE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Confirm Database Restore") },
            text = {
                Text("Are you sure you want to restore data from this backup? Existing records will be merged and updated safely.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreBackup(importJsonInput) { success, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                                if (success) {
                                    importJsonInput = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentPaid)
                ) {
                    Text("Confirm Restore", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
