package com.rosan.ruto.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.rosan.installer.ext.util.process.ProcessUtil
import com.rosan.ruto.service.KeepAliveService
import com.rosan.ruto.ui.Destinations
import com.rosan.ruto.util.SettingsManager
import kotlinx.coroutines.launch

private sealed interface GuideUiState {
    object Loading : GuideUiState
    object ShizukuPermissionGranted : GuideUiState
    object NotificationPermissionGranted : GuideUiState
    data class ShizukuPermissionNeeded(val message: String) : GuideUiState
    object NotificationPermissionNeeded : GuideUiState
    object ModelConfigNeeded : GuideUiState
    object AllSet : GuideUiState
}

@Composable
fun GuideScreen(navController: NavController, insets: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<GuideUiState>(GuideUiState.Loading) }
    var showModelConfigDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        uiState = if (it) {
            GuideUiState.NotificationPermissionGranted
        } else {
            GuideUiState.NotificationPermissionNeeded
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is GuideUiState.Loading -> {
                if (ProcessUtil.isShizukuPermissionsGranted()) {
                    uiState = GuideUiState.ShizukuPermissionGranted
                } else {
                    uiState = GuideUiState.ShizukuPermissionNeeded("需要 Shizuku 权限才能继续。")
                }
            }
            is GuideUiState.ShizukuPermissionGranted -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    uiState = GuideUiState.NotificationPermissionNeeded
                } else {
                    uiState = GuideUiState.NotificationPermissionGranted
                }
            }
            is GuideUiState.NotificationPermissionGranted -> {
                KeepAliveService.start(context)

                uiState = GuideUiState.AllSet
//                if (SettingsManager.areSettingsConfigured(context)) {
//                    uiState = GuideUiState.AllSet
//                } else {
//                    uiState = GuideUiState.ModelConfigNeeded
//                }
            }
            is GuideUiState.AllSet -> {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.GUIDE) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(targetState = uiState, label = "GuideContent") { state ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state) {
                    is GuideUiState.Loading -> CircularProgressIndicator()
                    is GuideUiState.ShizukuPermissionGranted -> {
                        Icon(
                            imageVector = Icons.TwoTone.CheckCircle,
                            contentDescription = "Shizuku 权限已授予",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Shizuku 权限已授予。")
                    }
                    is GuideUiState.ShizukuPermissionNeeded -> {
                        Icon(
                            imageVector = Icons.TwoTone.ErrorOutline,
                            contentDescription = "需要 Shizuku 权限",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { scope.launch {
                            uiState = GuideUiState.Loading
                            runCatching { ProcessUtil.requestShizukuPermissions() }
                                .onSuccess { uiState = GuideUiState.ShizukuPermissionGranted }
                                .onFailure { throwable ->
                                    uiState = GuideUiState.ShizukuPermissionNeeded(
                                        throwable.message ?: "发生未知错误"
                                    )
                                }
                        } }) {
                            Text("授予 Shizuku 权限")
                        }
                    }
                    is GuideUiState.NotificationPermissionNeeded -> {
                        Icon(
                            imageVector = Icons.TwoTone.Notifications,
                            contentDescription = "需要通知权限",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "保活服务需要通知权限。")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("授予通知权限")
                        }
                    }
                    is GuideUiState.NotificationPermissionGranted -> {
                        Icon(
                            imageVector = Icons.TwoTone.CheckCircle,
                            contentDescription = "通知权限已授予",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "通知权限已授予。")
                    }
                    is GuideUiState.ModelConfigNeeded -> {
                        Icon(
                            imageVector = Icons.TwoTone.Settings,
                            contentDescription = "需要配置模型",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "请先配置 AI 模型。")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showModelConfigDialog = true }) {
                            Text("去配置模型")
                        }
                    }
                    is GuideUiState.AllSet -> {
                        Icon(
                            imageVector = Icons.TwoTone.CheckCircle,
                            contentDescription = "全部就绪",
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "一切就绪，正在跳转…")
                    }
                }
            }
        }
    }

    if (showModelConfigDialog) {
        ModelConfigDialog(
            onDismiss = { showModelConfigDialog = false },
            onConfirm = {
                showModelConfigDialog = false
                uiState = GuideUiState.AllSet
            }
        )
    }
}

@Composable
private fun ModelConfigDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    var hostUrl by remember { mutableStateOf(SettingsManager.getHostUrl(context)) }
    var apiKey by remember { mutableStateOf(SettingsManager.getApiKey(context)) }
    var modelId by remember { mutableStateOf(SettingsManager.getModelId(context)) }

    var isHostUrlError by remember { mutableStateOf(false) }
    var isApiKeyError by remember { mutableStateOf(false) }
    var isModelIdError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hostUrl,
                    onValueChange = { hostUrl = it; isHostUrlError = false },
                    label = { Text("接口地址") },
                    isError = isHostUrlError
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; isApiKeyError = false },
                    label = { Text("API 密钥") },
                    isError = isApiKeyError
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it; isModelIdError = false },
                    label = { Text("模型 ID") },
                    isError = isModelIdError
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                isHostUrlError = hostUrl.isBlank()
                isApiKeyError = apiKey.isBlank()
                isModelIdError = modelId.isBlank()
                if (!isHostUrlError && !isApiKeyError && !isModelIdError) {
                    SettingsManager.saveSettings(context, hostUrl, apiKey, modelId)
                    onConfirm()
                }
            }) {
                Text("确认")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
