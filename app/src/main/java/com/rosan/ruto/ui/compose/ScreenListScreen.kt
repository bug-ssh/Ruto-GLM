package com.rosan.ruto.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.twotone.HourglassTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val navigateToMultiTask = { ids: List<Int> ->
        if (ids.isNotEmpty()) {
            navController.navigate("${Destinations.MULTI_TASK_PREVIEW}/${ids.joinToString(",")}")
        }
    }

    fun toggleSelection(displayId: Int) {
        selectedDisplayIds = if (displayId in selectedDisplayIds)
            selectedDisplayIds - displayId else selectedDisplayIds + displayId
    }

    LaunchedEffect(Unit) { viewModel.loadDisplays() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isInSelectionMode,
                transitionSpec = {
                    if (targetState) slideInVertically { it } togetherWith slideOutVertically { -it }
                    else slideInVertically { -it } togetherWith slideOutVertically { it }
                },
                label = "TopAppBarSwitch"
            ) { selectionMode ->
                if (selectionMode) {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text("已选 ${selectedDisplayIds.size} 项", fontWeight = FontWeight.Bold)
                                Text(
                                    "长按可多选，点击预览",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedDisplayIds = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "取消选择")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectedDisplayIds = uiState.displays.map { it.displayId }.toSet()
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                            }
                            AnimatedVisibility(visible = selectedDisplayIds.isNotEmpty()) {
                                IconButton(onClick = {
                                    selectedDisplayIds.forEach { viewModel.release(it) }
                                    selectedDisplayIds = emptySet()
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "批量删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text("屏幕管理", fontWeight = FontWeight.Bold)
                                Text(
                                    "管理虚拟屏幕与设备屏幕",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = isInSelectionMode) {
                    FloatingActionButton(
                        onClick = { navigateToMultiTask(selectedDisplayIds.toList()) },
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
            when {
                uiState.displays.isEmpty() && uiState.isRefreshing -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "loading")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                        label = "scale"
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.TwoTone.HourglassTop,
                            contentDescription = "加载中…",
                            modifier = Modifier.scale(scale)
                        )
                    }
                }
                uiState.displays.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.HourglassTop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "暂无屏幕，点击右下角新建",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp
                        )
                    ) {
                        items(uiState.displays, key = { it.displayId }) { display ->
                            ScreenListItem(
                                display = display,
                                isSelected = display.displayId in selectedDisplayIds,
                                onDelete = { viewModel.release(display.displayId) },
                                onPreview = { navigateToMultiTask(listOf(display.displayId)) },
                                onClick = {
                                    if (isInSelectionMode) toggleSelection(display.displayId)
                                    else navigateToMultiTask(listOf(display.displayId))
                                },
                                onLongClick = { toggleSelection(display.displayId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
