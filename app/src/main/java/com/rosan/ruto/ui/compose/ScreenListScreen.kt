package com.rosan.ruto.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.twotone.HourglassTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.ruto.ui.Destinations
import com.rosan.ruto.ui.compose.screen_list.CreateDisplayDialog
import com.rosan.ruto.ui.compose.screen_list.ScreenListItem
import com.rosan.ruto.ui.viewmodel.ScreenListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenListScreen(navController: NavController, insets: WindowInsets) {
    val viewModel: ScreenListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDisplayIds by remember { mutableStateOf(emptySet<Int>()) }
    val isInSelectionMode = selectedDisplayIds.isNotEmpty()

    val navigateToMultiTask = { ids: List<Int> ->
        if (ids.isNotEmpty()) {
            val idsString = ids.joinToString(",")
            navController.navigate("${Destinations.MULTI_TASK_PREVIEW}/$idsString")
        }
    }

    fun toggleSelection(displayId: Int) {
        selectedDisplayIds = if (displayId in selectedDisplayIds) {
            selectedDisplayIds - displayId
        } else {
            selectedDisplayIds + displayId
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDisplays()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isInSelectionMode) "已选 ${selectedDisplayIds.size} 项" else "屏幕列表") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        IconButton(onClick = {
                            selectedDisplayIds = uiState.displays.map { it.displayId }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = isInSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            // 修改：点击悬浮按钮预览选中的多个屏幕
                            navigateToMultiTask(selectedDisplayIds.toList())
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "预览已选")
                    }
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建屏幕")
                }
            }
        },
        contentWindowInsets = insets
    ) { padding ->
        if (showCreateDialog) {
            CreateDisplayDialog(
                uiState = uiState,
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, width, height, density ->
                    viewModel.createDisplay(name, width, height, density)
                    showCreateDialog = false
                }
            )
        }

        PullToRefreshBox(
            modifier = Modifier.padding(padding),
            isRefreshing = uiState.displays.isNotEmpty() && uiState.isRefreshing,
            onRefresh = { viewModel.loadDisplays() }
        ) {
            if (uiState.displays.isEmpty() && uiState.isRefreshing) {
                val infiniteTransition =
                    rememberInfiniteTransition(label = "loading_indicator_scale")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ), label = "scale"
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.TwoTone.HourglassTop,
                        contentDescription = "加载中…",
                        modifier = Modifier.scale(scale)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(uiState.displays, key = { it.displayId }) { display ->
                        ScreenListItem(
                            display = display,
                            isSelected = display.displayId in selectedDisplayIds,
                            onDelete = { viewModel.release(display.displayId) },
                            // 修改：预览按钮跳转多任务预览（单元素列表）
                            onPreview = { navigateToMultiTask(listOf(display.displayId)) },
                            onClick = {
                                if (isInSelectionMode) {
                                    toggleSelection(display.displayId)
                                } else {
                                    // 修改：普通点击也跳转多任务预览（单元素列表）
                                    navigateToMultiTask(listOf(display.displayId))
                                }
                            },
                            onLongClick = {
                                toggleSelection(display.displayId)
                            }
                        )
                    }
                }
            }
        }
    }
}