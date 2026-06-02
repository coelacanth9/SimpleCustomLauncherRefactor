package com.coelacanth9.simplecustomlauncher.data

import java.util.UUID

data class MemoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class MemoSettings(
    val fontSize: Int = 20
)
