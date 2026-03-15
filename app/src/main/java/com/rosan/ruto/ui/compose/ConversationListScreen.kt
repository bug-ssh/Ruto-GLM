package com.rosan.ruto.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.twotone.Forum
import androidx.compose.material.icons.twotone.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.ruto.data.model.AiModel
import com.rosan.ruto.data.model.ConversationModel
import com.rosan.ruto.ui.Destinations
import com.rosan.ruto.ui.viewmodel.ConversationListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    navController: NavController,
    insets: WindowInsets,
    viewModel: ConversationListViewModel = koinViewModel()
) {
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())
    val aiModels by viewModel.aiModels.collectAsState(initial = emptyList())
    val isLoading by remember { mutableStateOf(false) }

    var selectedIds by remember { mutableStateOf(emptyList<Long>()) }
    val isInSelectionMode = selectedIds.isNotEmpty()

    var showDialog by remember { mutableStateOf(false) }

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isInSelectionMode, transitionSpec = {
                    if (targetState) {
                        slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                    } else {
                        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                    }
                }, label = "TopAppBar"
            ) { selectionModeActive ->
                if (selectionModeActive) {
                    TopAppBar(title = {
                        AnimatedContent(
                            targetState = selectedIds.size, transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith (slideOutVertically { height -> height } + fadeOut())
                                }.using(SizeTransform(clip = false))
                            }, label = "TextPushAnimation"
                        ) { targetSize ->
                            Text(text = "$targetSize selected")
                        }
                    }, navigationIcon = {
                        IconButton(onClick = { selectedIds = emptyList() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }, actions = {
                        IconButton(onClick = { selectedIds = conversations.map { it.id } }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    })
                } else {
                    TopAppBar(title = { Text("对话列表") }, navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                            )
                        }
                    })
                }
            }
        }, floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isInSelectionMode) {
                            if (selectedIds.isEmpty()) return@FloatingActionButton
                            viewModel.remove(selectedIds)
                            selectedIds = emptyList()
                        } else showDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    AnimatedContent(
                        targetState = isInSelectionMode, transitionSpec = {
                            if (targetState) {
                                slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                            } else {
                                slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                            }
                        }, label = "floatingActionButton"
                    ) { selectionMode ->
                        if (selectionMode) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        }, contentWindowInsets = insets
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (conversations.isEmpty()) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    EmptyConversation()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        // 增加 animateItem 使得增删列表时有位移动画
                        Box(modifier = Modifier.animateItem()) {
                            ConversationListItem(
                                aiModels = aiModels,
                                conversation = conversation,
                                isSelected = conversation.id in selectedIds,
                                onClick = {
                                    if (isInSelectionMode) toggleSelection(conversation.id)
                                    else navController.navigate("${Destinations.CONVERSATION}/${conversation.id}")
                                },
                                onLongClick = { toggleSelection(conversation.id) })
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateConversationDialog(
            aiModels = aiModels,
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.add(it)
                showDialog = false
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateConversationDialog(
    aiModels: List<AiModel>,
    onDismiss: () -> Unit,
    onConfirm: (conversationModel: ConversationModel) -> Unit,
) {
    var name by remember { mutableStateOf("新对话") }
    var aiId by remember { mutableStateOf<Long?>(null) }

    var isTaskConversation by remember { mutableStateOf(false) }
    var selectedDisplayId by remember { mutableStateOf<Int?>(null) }

    val device = koinInject<com.rosan.ruto.device.DeviceManager>()
    var displays by remember { mutableStateOf(emptyList<android.view.DisplayInfo>()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        displays = device.getDisplayManager().displays
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建对话") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("AI 模型", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    aiModels.forEach { aiModel ->
                        FilterChip(
                            selected = aiModel.id == aiId,
                            onClick = { aiId = aiModel.id },
                            label = { Text(aiModel.name) },
                            leadingIcon = {
                                if (aiModel.id == aiId) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("任务模式", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "启用指定屏幕自动化",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTaskConversation,
                        onCheckedChange = {
                            isTaskConversation = it
                            if (!it) selectedDisplayId = null
                        }
                    )
                }

                AnimatedVisibility(visible = isTaskConversation) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("选择目标屏幕", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            displays.forEach { display ->
                                @Composable
                                fun DisplayChip(displayId: Int, displayName: String) {
                                    FilterChip(
                                        selected = selectedDisplayId == displayId,
                                        onClick = { selectedDisplayId = displayId },
                                        label = { Text(displayName) },
                                        leadingIcon = {
                                            if (selectedDisplayId == displayId) {
                                                Icon(
                                                    Icons.Default.Monitor,
                                                    null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                                DisplayChip(
                                    display.displayId,
                                    "屏幕 ${display.displayId}（${display.name}）"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    aiId?.let {
                        val model = ConversationModel(
                            aiId = it,
                            name = name,
                            displayId = selectedDisplayId
                        )
                        onConfirm(model)
                    }
                },
                // 校验逻辑：必须选了 AI；如果是任务模式，必须选了屏幕
                enabled = aiId != null && (!isTaskConversation || selectedDisplayId != null)
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListItem(
    aiModels: List<AiModel>,
    conversation: ConversationModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 0.96f else 1f, label = "scale")
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surface
        }, label = "backgroundColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        ListItem(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .combinedClickable(
                    onClick = onClick, onLongClick = onLongClick
                ),
            headlineContent = {
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    label = "contentColor"
                )
                Text(
                    conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = animatedColor
                )
            }, supportingContent = {
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    label = "contentColor"
                )
                Text(
                    "模型：${aiModels.find { it.id == conversation.aiId }?.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedColor
                )
            }, leadingContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isSelected,
                        label = "iconFade"
                    ) { selected ->
                        Icon(
                            imageVector = if (selected) Icons.Default.SelectAll else Icons.TwoTone.HourglassTop,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            })
    }
}

@Composable
private fun LoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            Icons.TwoTone.HourglassTop,
            contentDescription = null,
            modifier = Modifier.scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmptyConversation() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.TwoTone.Forum,
                contentDescription = "暂无对话",
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Text(
                text = "暂无对话，点击右下角新建吧！",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}