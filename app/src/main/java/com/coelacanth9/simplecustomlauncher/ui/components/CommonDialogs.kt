package com.coelacanth9.simplecustomlauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 確認ダイアログ（大きめテキスト・タイトル+本文）。
 * TODO: Phase6 で旧プロジェクトを参考にスクロール対応・デザイン実装予定
 */
@Composable
fun LargeConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelText) }
        }
    )
}

/**
 * 利用規約同意ダイアログ。
 * required=true のとき onDismissRequest を無効化する。
 * TODO: Phase6 で旧プロジェクトを参考にデザイン実装予定
 */
@Composable
fun TermsConsentDialog(
    required: Boolean,
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!required) onDismiss() },
        title = { Text("利用規約") },
        text = { Text("利用規約の内容をご確認ください。") },
        confirmButton = {
            TextButton(onClick = onAgree) { Text("同意する") }
        },
        dismissButton = {
            if (!required) {
                TextButton(onClick = onDismiss) { Text("あとで") }
            }
        }
    )
}
