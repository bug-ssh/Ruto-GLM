package com.rosan.ruto.ui.compose.screen_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rosan.ruto.ui.viewmodel.ScreenListUiState

/**
 * 新建屏幕的输入对话框
 */
@Composable
fun CreateDisplayDialog(
    uiState: ScreenListUiState,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int) -> Unit
) {
    val defaultDisplay = uiState.displays.firstOrNull()
    val defaultWidth = defaultDisplay?.logicalWidth ?: 1080
    val defaultHeight = defaultDisplay?.logicalHeight ?: 1920
    val defaultDensity = defaultDisplay?.logicalDensityDpi ?: 420
    var name by remember { mutableStateOf("虚拟屏幕") }
    var width by remember { mutableStateOf("$defaultWidth") }
    var height by remember { mutableStateOf("$defaultHeight") }
    var density by remember { mutableStateOf("$defaultDensity") }

    // 实时逻辑校验
    val nameError = name.isBlank()
    val widthInt = width.toIntOrNull() ?: 0
    val heightInt = height.toIntOrNull() ?: 0
    val densityInt = density.toIntOrNull() ?: 0

    val isFormValid = !nameError && widthInt > 0 && heightInt > 0 && densityInt > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建虚拟屏幕") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("屏幕名称") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = width,
                    onValueChange = { if (it.all { c -> c.isDigit() }) width = it },
                    label = { Text("宽度 (px)") },
                    isError = widthInt <= 0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                    label = { Text("高度 (px)") },
                    isError = heightInt <= 0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = density,
                    onValueChange = { if (it.all { c -> c.isDigit() }) density = it },
                    label = { Text("像素密度 (DPI)") },
                    isError = densityInt <= 0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, widthInt, heightInt, densityInt) },
                enabled = isFormValid
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}