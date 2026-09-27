package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.viewmodel.RepairViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RepairViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentSettings by viewModel.settings.collectAsState()

    var shopName by remember(currentSettings) { mutableStateOf(currentSettings.shopName) }
    var shopPhone by remember(currentSettings) { mutableStateOf(currentSettings.shopPhone) }
    var shopAddress by remember(currentSettings) { mutableStateOf(currentSettings.shopAddress) }
    var whatsappNumber by remember(currentSettings) { mutableStateOf(currentSettings.whatsappNumber) }
    var receiptFooter by remember(currentSettings) { mutableStateOf(currentSettings.receiptFooter) }
    var currency by remember(currentSettings) { mutableStateOf(currentSettings.currency) }
    var startingJobNumberStr by remember(currentSettings) { mutableStateOf(currentSettings.startingJobNumber.toString()) }
    var autoReadySms by remember(currentSettings) { mutableStateOf(currentSettings.autoReadySms) }
    var smsTemplate by remember(currentSettings) { mutableStateOf(currentSettings.smsTemplate) }
    var isDarkMode by remember(currentSettings) { mutableStateOf(currentSettings.isDarkMode) }

    val defaultTemplate = "UDM Mobile Repair\n\nDear {customer_name}, your phone repair Job #{job_number} is ready for collection. Total repair price: Rs. {total_price}. Balance: Rs. {balance}. Thank you."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Button(
                    onClick = {
                        val startingNum = startingJobNumberStr.toIntOrNull() ?: 1
                        val updated = currentSettings.copy(
                            shopName = shopName.trim(),
                            shopPhone = shopPhone.trim(),
                            shopAddress = shopAddress.trim(),
                            whatsappNumber = whatsappNumber.trim(),
                            receiptFooter = receiptFooter.trim(),
                            currency = currency.trim(),
                            startingJobNumber = startingNum,
                            autoReadySms = autoReadySms,
                            smsTemplate = smsTemplate.trim(),
                            isDarkMode = isDarkMode
                        )
                        viewModel.updateSettings(updated)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Settings saved successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                        .testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF090E1A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE SETTINGS", color = Color(0xFF090E1A), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SHOP INFORMATION
            item {
                SectionCard(title = "SHOP INFORMATION") {
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setting_shop_name")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it },
                        label = { Text("Shop Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("Shop WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Shop Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency Symbol") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startingJobNumberStr,
                            onValueChange = { startingJobNumberStr = it },
                            label = { Text("Starting Job #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Receipt Footer Note / Warranty") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // SMS SETTINGS
            item {
                SectionCard(title = "READY FOR COLLECTION SMS SETTINGS") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Ready SMS", fontWeight = FontWeight.Bold)
                            Text(
                                "Send normal SMS to customer phone when repair status is marked READY",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoReadySms,
                            onCheckedChange = { autoReadySms = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = smsTemplate,
                        onValueChange = { smsTemplate = it },
                        label = { Text("SMS Message Template") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("setting_sms_template")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Dynamic Tags: {customer_name}, {job_number}, {total_price}, {balance}, {model}, {shop_name}, {shop_phone}",
                        fontSize = 11.sp,
                        color = CyanAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { smsTemplate = defaultTemplate },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Default Template", color = CyanAccent, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Preview
                    Text("SAMPLE SMS PREVIEW:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sampleRendered = smsTemplate
                            .replace("{customer_name}", "Kasun")
                            .replace("{job_number}", "0014")
                            .replace("{total_price}", "5,500")
                            .replace("{balance}", "2,000")
                            .replace("{model}", "Samsung Galaxy M02")
                            .replace("{shop_name}", shopName)
                            .replace("{shop_phone}", shopPhone)

                        Text(
                            text = sampleRendered,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // APPEARANCE
            item {
                SectionCard(title = "APPEARANCE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Navy Theme", fontWeight = FontWeight.Bold)
                            Text("Professional dark mode for repair shops", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }
            }
        }
    }
}
