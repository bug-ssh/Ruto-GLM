package com.rosan.ruto.ui.compose

import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.ruto.ui.Destinations
import com.rosan.ruto.util.SettingsManager
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

@Composable
fun SplashScreen(navController: NavController, insets: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1500),
        label = "splash_animation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500)

        // 检查是否已配置授权方式
        val provider = SettingsManager.getPermissionProvider(context)

        val destination = if (provider != null) {
            // 已选过授权方式：检查 Shizuku 权限是否仍然有效
            val shizukuOk = try {
                Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }

            if (shizukuOk) {
                // Shizuku 权限仍然有效，直接跳过引导页进入主界面
                Destinations.HOME
            } else {
                // 需要重新授权
                Destinations.GUIDE
            }
        } else {
            // 首次启动，进入引导页
            Destinations.GUIDE
        }

        navController.navigate(destination) {
            popUpTo(Destinations.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(insets),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.TwoTone.AutoAwesome,
            contentDescription = "Splash Screen Icon",
            modifier = Modifier.scale(scale.value)
        )
    }
}
