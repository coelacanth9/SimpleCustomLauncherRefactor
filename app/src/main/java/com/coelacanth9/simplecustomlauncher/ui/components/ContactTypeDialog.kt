package com.coelacanth9.simplecustomlauncher.ui.components

import com.coelacanth9.simplecustomlauncher.R
import com.coelacanth9.simplecustomlauncher.model.RAKUTEN_LINK_PACKAGE
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap

/**
 * 連絡先の種類選択ダイアログ（電話 / SMS / Link通話 / Link SMS）
 */
@Composable
fun ContactTypeDialog(
    contactName: String,
    onSelectPhone: () -> Unit,
    onSelectSms: () -> Unit,
    onSelectLinkPhone: (() -> Unit)? = null,
    onSelectLinkSms: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val smsIcon = remember {
        val pm = context.packageManager
        listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging"
        ).firstNotNullOfOrNull { pkg ->
            try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
        }
    }

    val linkIcon = remember {
        if (onSelectLinkPhone != null || onSelectLinkSms != null) {
            try { context.packageManager.getApplicationIcon(RAKUTEN_LINK_PACKAGE) }
            catch (e: Exception) { null }
        } else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = contactName, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.select_contact_type),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 電話
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectPhone() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.shortcut_type_call),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.tap_to_call),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SMS
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectSms() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (smsIcon != null) {
                            val bitmap = remember(smsIcon) { smsIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.shortcut_type_sms),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.tap_to_message),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Link通話（楽天Linkインストール時のみ）
                if (onSelectLinkPhone != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectLinkPhone() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (linkIcon != null) {
                                val bitmap = remember(linkIcon) { linkIcon.toBitmap(64, 64) }
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                            } else {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = stringResource(R.string.link_call), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text(text = stringResource(R.string.tap_to_call_link), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // LinkSMS（楽天Linkインストール時のみ）
                if (onSelectLinkSms != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectLinkSms() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (linkIcon != null) {
                                val bitmap = remember(linkIcon) { linkIcon.toBitmap(64, 64) }
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                            } else {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = stringResource(R.string.link_sms), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text(text = stringResource(R.string.tap_to_message_link_contact), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
