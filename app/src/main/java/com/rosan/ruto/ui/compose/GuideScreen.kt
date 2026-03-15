package com.rosan.ruto.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.rosan.ruto.device.DeviceManager
import com.rosan.ruto.service.KeepAliveService
import com.rosan.ruto.ui.Destinations
import com.rosan.ruto.util.PermissionProvider
import com.rosan.ruto.util.SettingsManager
import org.koin.compose.currentKoinScope

private sealed interface GuideUiState {
    object Loading : GuideUiState
    object PermissionSelection : GuideUiState
    data class PermissionNeeded(val provider: PermissionProvider, val message: String) : GuideUiState
    data class PermissionGranted(val provider: PermissionProvider) : GuideUiState
    object NotificationPermissionNeeded : GuideUiState
    object NotificationPermissionGranted : GuideUiState
    object AllSet : GuideUiState
}

@Composable
fun GuideScreen(navController: NavController, insets: PaddingValues) {
    val context = LocalContext.current
    val koinScope = currentKoinScope()
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<GuideUiState>(GuideUiState.Loading) }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            uiState = if (it) {
                GuideUiState.NotificationPermissionGranted
            } else {
                GuideUiState.NotificationPermissionNeeded
            }
        }

    LaunchedEffect(uiState) {
        when (uiState) {
            is GuideUiState.Loading -> {
                val provider = SettingsManager.getPermissionProvider(context)
                if (provider != null) {
                    runCatching {
                        koinScope.get<DeviceManager>().serviceManager.ensureConnected()
                    }.onSuccess {
                        uiState = GuideUiState.PermissionGranted(provider)
                    }.onFailure { throwable ->
                        uiState = GuideUiState.PermissionNeeded(
                            provider,
                            throwable.message ?: "连接权限服务失败。"
                        )
                    }
                } else {
                    uiState = GuideUiState.PermissionSelection
                }
            }

            is GuideUiState.PermissionGranted -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    uiState = GuideUiState.NotificationPermissionNeeded
                } else {
                    uiState = GuideUiState.NotificationPermissionGranted
                }
            }

            is GuideUiState.NotificationPermissionGranted -> {
                KeepAliveService.start(context)
                uiState = GuideUiState.AllSet
            }

            is GuideUiState.AllSet -> {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.GUIDE) { inclusive = true }
                }
            }

            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = uiState,
                label = "GuideContent",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) + expandVertically()) togetherWith
                            (fadeOut(animationSpec = tween(500)) + shrinkVertically())
                }
            ) { state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (state) {
                        is GuideUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "正在检查权限…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is GuideUiState.PermissionSelection -> {
                            var selectedProvider by remember { mutableStateOf<PermissionProvider?>(null) }
                            var shell by remember {
                                mutableStateOf(SettingsManager.getTerminalShell(context))
                            }
                            var isShellError by remember { mutableStateOf(false) }

                            Text(
                                "欢迎使用 Ruto",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "请选择授权方式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                                textAlign = TextAlign.Center
                            )

                            Column(
                                modifier = Modifier
                                    .selectableGroup()
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PermissionProvider.entries.forEach { provider ->
                                    val isSelected = selectedProvider == provider
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedProvider = provider },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(20.dp)
                                                .fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null
                                            )
                                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                                Text(
                                                    text = when (provider) {
                                                        PermissionProvider.SHIZUKU -> "Shizuku"
                                                        PermissionProvider.SHIZUKU_TERMINAL -> "Shizuku Terminal"
                                                        PermissionProvider.ROOT -> "Root 授权"
                                                        PermissionProvider.TERMINAL -> "终端"
                                                    },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = when (provider) {
                                                        PermissionProvider.SHIZUKU -> "Requires Shizuku app"
                                                        PermissionProvider.SHIZUKU_TERMINAL -> "Shizuku with terminal fallback"
                                                        PermissionProvider.ROOT -> "传统 Root 授权"
                                                        PermissionProvider.TERMINAL -> "手动 Shell 执行"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = selectedProvider == PermissionProvider.TERMINAL,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = shell,
                                        onValueChange = { shell = it; isShellError = false },
                                        label = { Text("Shell Path") },
                                        isError = isShellError,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    if (selectedProvider != null) {
                                        SettingsManager.savePermissionProvider(
                                            context,
                                            selectedProvider!!,
                                            if (selectedProvider == PermissionProvider.TERMINAL) shell else null
                                        )
                                        uiState = GuideUiState.Loading
                                    }
                                },
                                enabled = selectedProvider != null
                            ) {
                                Text("继续", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        is GuideUiState.PermissionNeeded -> {
                            Icon(
                                imageVector = Icons.TwoTone.ErrorOutline,
                                contentDescription = "Error",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = when (state.provider) {
                                    PermissionProvider.SHIZUKU -> "需要 Shizuku 服务"
                                    PermissionProvider.SHIZUKU_TERMINAL -> "需要 Shizuku Terminal 服务"
                                    PermissionProvider.ROOT -> "需要 Root 权限"
                                    PermissionProvider.TERMINAL -> "需要配置终端"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    uiState = GuideUiState.Loading
                                }
                            ) {
                                Text(
                                    text = when (state.provider) {
                                        PermissionProvider.SHIZUKU -> "授予 Shizuku 权限"
                                        PermissionProvider.SHIZUKU_TERMINAL -> "授予 Shizuku 权限"
                                        PermissionProvider.ROOT -> "授予 Root 权限"
                                        PermissionProvider.TERMINAL -> "重试终端连接"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    uiState = GuideUiState.PermissionSelection
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("切换授权方式", fontWeight = FontWeight.Bold)
                            }
                        }

                        is GuideUiState.PermissionGranted, is GuideUiState.NotificationPermissionGranted, is GuideUiState.AllSet -> {
                            Icon(
                                imageVector = Icons.TwoTone.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = when (state) {
                                    is GuideUiState.PermissionGranted -> "${state.provider.name} Ready"
                                    is GuideUiState.NotificationPermissionGranted -> "通知权限已开启"
                                    else -> "一切就绪！"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "权限已成功授予。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        is GuideUiState.NotificationPermissionNeeded -> {
                            Icon(
                                imageVector = Icons.TwoTone.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "通知权限",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "开启通知权限以保持服务稳定运行。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            ) {
                                Text("开启通知权限", fontWeight = FontWeight.Bold)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
