package com.rosan.ruto.ui.compose

import android.app.Activity
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.rosan.ruto.ui.compose.screen_preview.AppPickerDialog
import com.rosan.ruto.ui.compose.screen_preview.FloatingActionMenu
import com.rosan.ruto.ui.viewmodel.MultiTaskPreviewViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MultiTaskPreviewScreen(navController: NavController, displayIds: List<Int>) {
    val viewModel: MultiTaskPreviewViewModel = koinViewModel { parametersOf(displayIds) }
    val displaySizes by viewModel.displaySizes.collectAsState()
    val view = LocalView.current
    val density = LocalDensity.current

    val zIndexMap = remember {
        mutableStateMapOf<Int, Float>().apply {
            displayIds.forEachIndexed { index, id -> put(id, index.toFloat()) }
        }
    }
    var topZIndex by remember { mutableFloatStateOf(displayIds.size.toFloat()) }
    var activeDisplayId by remember { mutableStateOf<Int?>(displayIds.firstOrNull()) }
    var isDragMode by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1C1E))
            .pointerInput(Unit) { detectTapGestures { activeDisplayId = null } }
            .drawBehind {
                val step = 40.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) drawLine(
                    Color.White.copy(alpha = 0.05f),
                    Offset(x.toFloat(), 0f),
                    Offset(x.toFloat(), size.height),
                    1f
                )
                for (y in 0..size.height.toInt() step step.toInt()) drawLine(
                    Color.White.copy(alpha = 0.05f),
                    Offset(0f, y.toFloat()),
                    Offset(size.width, y.toFloat()),
                    1f
                )
            }) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        displayIds.forEachIndexed { index, displayId ->
            val remoteSize = displaySizes[displayId]
            if (remoteSize != null && remoteSize.width > 0) {
                val (winWidthDp, winHeightDp) = remember(remoteSize, maxWidth, maxHeight) {
                    val remoteAspect = remoteSize.width / remoteSize.height
                    val screenAspect = maxWidth.value / maxHeight.value
                    if (remoteAspect > screenAspect) Pair(maxWidth, maxWidth / remoteAspect)
                    else Pair(maxHeight * remoteAspect, maxHeight)
                }

                val initialOffset = remember(displayId) {
                    val wPx = with(density) { winWidthDp.toPx() }
                    val hPx = with(density) { winHeightDp.toPx() }
                    Offset(
                        (screenWidthPx - wPx) / 2f + index * 60f,
                        (screenHeightPx - hPx) / 2f + index * 60f
                    )
                }

                TaskWindow(
                    displayId = displayId,
                    zIndex = zIndexMap[displayId] ?: 0f,
                    isSelected = activeDisplayId == displayId,
                    isDragMode = isDragMode,
                    initialOffset = initialOffset,
                    width = winWidthDp,
                    height = winHeightDp,
                    remoteSize = remoteSize,
                    viewModel = viewModel,
                    onInteract = {
                        activeDisplayId = displayId
                        if (zIndexMap[displayId] != topZIndex) {
                            topZIndex += 1f
                            zIndexMap[displayId] = topZIndex
                        }
                    })
            }
        }

        if (showAppPickerDialog && activeDisplayId != null) {
            AppPickerDialog(onDismiss = { showAppPickerDialog = false }, onAppSelected = { pkg ->
                showAppPickerDialog = false
                activeDisplayId?.let { viewModel.launch(it, pkg) }
            })
        }

        FloatingActionMenu(
            subButtons = listOf(
                Icons.AutoMirrored.Filled.ArrowBack to "返回",
                Icons.Filled.Apps to "选择应用",
                (if (isDragMode) Icons.Filled.TouchApp else Icons.Filled.PanTool) to "切换模式",
                Icons.Filled.Close to "关闭"
            ),
            onButtonClick = { action ->
                when (action) {
                    "返回" -> activeDisplayId?.let { viewModel.clickBack(it) }
                    "选择应用" -> if (activeDisplayId != null) showAppPickerDialog = true
                    "切换模式" -> isDragMode = !isDragMode
                    "Close" -> navController.popBackStack()
                }
            },
            onButtonLongClick = {},
            isButtonEnabled = { action -> if (action == "关闭" || action == "切换模式") true else activeDisplayId != null },
            screenWidth = screenWidthPx,
            screenHeight = screenHeightPx
        )
    }
}

@Composable
fun TaskWindow(
    displayId: Int,
    zIndex: Float,
    isSelected: Boolean,
    isDragMode: Boolean,
    initialOffset: Offset,
    width: Dp,
    height: Dp,
    remoteSize: Size,
    viewModel: MultiTaskPreviewViewModel,
    onInteract: () -> Unit
) {
    // 状态管理
    var offset by remember(displayId) { mutableStateOf(initialOffset) }
    var scale by remember(displayId) { mutableFloatStateOf(1f) }
    var rotation by remember(displayId) { mutableFloatStateOf(0f) }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            // 使用 graphicsLayer 统一处理所有变换（包括位移），避免坐标系分裂
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
            .size(width, height)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Color.Black)
            .border(
                if (isSelected) 3.dp else 1.dp,
                animatedBorderColor,
                shape = MaterialTheme.shapes.extraLarge
            )
            // 拖动/缩放手势监听（父级处理）
            .pointerInput(displayId, isDragMode) {
                // 只有在拖动模式下，Compose 才接管手势
                if (isDragMode) {
                    detectTransformGestures { centroid, pan, zoom, rotationChange ->
                        onInteract() // 拖动开始即激活

                        // 计算变换
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(0.1f, 15f)
                        rotation += rotationChange

                        // 修正位移，确保缩放中心正确
                        val angleRad = rotation * (PI.toFloat() / 180f)
                        val cosA = cos(angleRad);
                        val sinA = sin(angleRad)
                        val rotatedPanX = pan.x * cosA - pan.y * sinA
                        val rotatedPanY = pan.x * sinA + pan.y * cosA

                        // 这里的 offset 只是逻辑值，实际应用在 graphicsLayer.translation
                        offset = offset + centroid * oldScale - centroid * scale + Offset(
                            rotatedPanX * oldScale, rotatedPanY * oldScale
                        )
                    }
                }
            }) {
        AndroidView(factory = { context ->
            TextureView(context).also { textureView ->
                textureView.isOpaque = true
                // 清除自身的矩阵，完全依赖 Compose 的 graphicsLayer
                textureView.setTransform(null)
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        st.setDefaultBufferSize(
                            remoteSize.width.toInt(), remoteSize.height.toInt()
                        )
                        Log.e("r0s","set surface")
                        viewModel.setSurface(displayId, Surface(st))
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        Log.e("r0s","null surface")
                        viewModel.setSurface(displayId, null)
                        return true
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                        st.setDefaultBufferSize(
                            remoteSize.width.toInt(), remoteSize.height.toInt()
                        )
                    }
                }
            }
        }, modifier = Modifier.fillMaxSize(), update = { textureView ->
            textureView.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onInteract()
                if (isDragMode) return@setOnTouchListener false

                val vw = v.width.toFloat()
                val vh = v.height.toFloat()
                if (vw <= 0 || vh <= 0) return@setOnTouchListener true

                fun transformMotionEvent(
                    event: MotionEvent, inverseMatrix: Matrix
                ): MotionEvent {
                    val pointerCount = event.pointerCount
                    val pointerProperties =
                        arrayOfNulls<MotionEvent.PointerProperties>(pointerCount)
                    val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(pointerCount)

                    for (i in 0 until pointerCount) {
                        // 获取原始属性
                        val prop = MotionEvent.PointerProperties()
                        event.getPointerProperties(i, prop)
                        pointerProperties[i] = prop

                        // 获取原始坐标并应用逆矩阵
                        val coords = MotionEvent.PointerCoords()
                        event.getPointerCoords(i, coords)

                        val pts = floatArrayOf(coords.x, coords.y)
                        inverseMatrix.mapPoints(pts)

                        coords.x = pts[0]
                        coords.y = pts[1]
                        pointerCoords[i] = coords
                    }

                    return MotionEvent.obtain(
                        event.downTime,
                        event.eventTime,
                        event.action,
                        pointerCount,
                        pointerProperties.requireNoNulls(),
                        pointerCoords.requireNoNulls(),
                        event.metaState,
                        event.buttonState,
                        event.xPrecision,
                        event.yPrecision,
                        displayId,
                        event.edgeFlags,
                        event.source,
                        event.flags
                    )
                }

                val matrix = Matrix()
                val xScale = (vw / remoteSize.width) * scale
                val yScale = (vh / remoteSize.height) * scale
                matrix.postScale(xScale, yScale, 0f, 0f)
                matrix.postRotate(rotation)
                val inverse = Matrix()
                if (matrix.invert(inverse)) {
                    val transformedEvent = transformMotionEvent(event, inverse)
                    viewModel.injectEvent(displayId, transformedEvent)
                    transformedEvent.recycle()
                }
                true
            }
        })
    }
}