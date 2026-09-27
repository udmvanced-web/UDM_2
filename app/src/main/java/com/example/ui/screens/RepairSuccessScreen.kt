package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RepairEntity
import com.example.ui.components.PaymentBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PaymentPaid
import com.example.ui.theme.PaymentPartial
import com.example.ui.util.safeGetItemsAndPayments
import com.example.ui.viewmodel.RepairViewModel
import com.example.util.ReceiptHelper
import java.util.Locale

@Composable
fun RepairSuccessScreen(
    repair: RepairEntity,
    viewModel: RepairViewModel,
    onDone: () -> Unit,
    onEdit: (Long) -> Unit
) {
    BackHandler { onDone() }

    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val (items, payments) = safeGetItemsAndPayments(repair.id, viewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PaymentPaid.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PaymentPaid,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "REPAIR REGISTERED!",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // VERY LARGE JOB NUMBER
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyanAccent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "JOB NUMBER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = repair.jobNumber,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Write this 4-digit number on customer sticker",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SUMMARY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(label = "Customer Name", value = repair.customerName)
                DetailRow(label = "Device", value = "${repair.brand} ${repair.model}")
                DetailRow(
                    label = "Total Price",
                    value = "${settings.currency} ${String.format(Locale.US, "%,.0f", repair.totalPrice)}"
                )
                DetailRow(
                    label = "Amount Paid",
                    value = "${settings.currency} ${String.format(Locale.US, "%,.0f", repair.amountPaid)}"
                )
                DetailRow(
                    label = "Balance Due",
                    value = "${settings.currency} ${String.format(Locale.US, "%,.0f", repair.balance)}",
                    valueColor = if (repair.balance > 0) PaymentPartial else CyanAccent
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Status:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    PaymentBadge(paymentStatus = repair.paymentStatus)
                }
                DetailRow(label = "Date & Time", value = "${repair.receivedDate} at ${repair.receivedTime}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ACTION BUTTONS: Print, Share, Edit, Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    ReceiptHelper.printReceipt(context, repair, items, payments, settings)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("success_print_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print", color = CyanAccent)
            }

            OutlinedButton(
                onClick = {
                    ReceiptHelper.shareReceipt(context, repair, items, payments, settings)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("success_share_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", color = CyanAccent)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onEdit(repair.id) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("success_edit_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Job")
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .testTag("success_done_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF090E1A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Done", color = Color(0xFF090E1A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = valueColor, fontSize = 13.sp)
    }
}
