package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SmartNoteUtils
import java.util.Calendar

@Composable
fun ReminderDialog(
    currentReminder: Long?,
    onSetReminder: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val now = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Set Note Reminder")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentReminder != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Current: ${SmartNoteUtils.formatTimestamp(currentReminder)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                        onSetReminder(c.timeInMillis)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("In 1 Hour")
                }

                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 20)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        if (c.timeInMillis <= System.currentTimeMillis()) {
                            c.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        onSetReminder(c.timeInMillis)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tonight at 8:00 PM")
                }

                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        onSetReminder(c.timeInMillis)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tomorrow Morning at 9:00 AM")
                }

                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        onSetReminder(c.timeInMillis)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next Week (7 days)")
                }
            }
        },
        confirmButton = {
            if (currentReminder != null) {
                Button(
                    onClick = { onSetReminder(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Reminder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
