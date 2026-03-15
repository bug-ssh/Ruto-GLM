package com.rosan.ruto.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.ruto.data.model.AiModel
import com.rosan.ruto.data.model.ai_model.AiCapability
import com.rosan.ruto.data.model.ai_model.AiType
import com.rosan.ruto.ui.viewmodel.AiModelListViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun LlmModelListScreen(navController: NavController, insets: WindowInsets) {
    val viewModel: AiModelListViewModel = koinViewModel()

    val models by remember(viewModel.models) {
        viewModel.models.distinctUntilChanged()
    }.collectAsState(initial = emptyList())

    var showModelEditorDialog by remember { mutableStateOf(false) }
    var modelToEdit by remember { mutableStateOf<AiModel?>(null) }
    var selectedIds by remember { mutableStateOf(emptyList<Long>()) }
    val isInSelectionMode = selectedIds.isNotEmpty()

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
                            Text(text = "已选 $targetSize 项")
                        }
                    }, navigationIcon = {
                        IconButton(onClick = { selectedIds = emptyList() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }, actions = {
                        IconButton(onClick = { selectedIds = models.map { it.id } }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                    })
                } else {
                    TopAppBar(title = { Text("AI 模型列表") }, navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回"
                            )
                        }
                    })
                }
            }
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isInSelectionMode) {
                        if (selectedIds.isEmpty()) return@FloatingActionButton
                        viewModel.remove(selectedIds)
                        selectedIds = emptyList()
                    } else {
                        modelToEdit = null
                        showModelEditorDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }
        }, contentWindowInsets = insets
    ) { padding ->
        if (models.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无模型，点击右下角添加吧！", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(models, key = { it.id }) { model ->
                    Box(modifier = Modifier.animateItem()) {
                        ModelItem(model = model, isSelected = model.id in selectedIds, onClick = {
                            if (isInSelectionMode) {
                                toggleSelection(model.id)
                            } else {
                                modelToEdit = model
                                showModelEditorDialog = true
                            }
                        }, onLongClick = {
                            toggleSelection(model.id)
                        })
                    }
                }
            }
        }
    }

    if (showModelEditorDialog) {
        ModelEditorDialog(
            viewModel = viewModel,
            modelToEdit = modelToEdit,
            onDismiss = { showModelEditorDialog = false },
            onConfirm = {
                showModelEditorDialog = false
            })
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ModelItem(
    model: AiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick, onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        model.modelId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val typeColor =
                        if (model.type == AiType.OPENAI) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                    Surface(
                        color = typeColor.copy(alpha = 0.15f), shape = CircleShape
                    ) {
                        Text(
                            text = model.type.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    model.capabilities.forEach { cap ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = cap.displayName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun ModelEditorDialog(
    viewModel: AiModelListViewModel,
    modelToEdit: AiModel? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isEditing = modelToEdit != null
    var name by remember { mutableStateOf(modelToEdit?.name ?: "新模型") }
    var baseUrl by remember { mutableStateOf(modelToEdit?.baseUrl ?: "https://api.openai.com/v1/") }
    var modelId by remember { mutableStateOf(modelToEdit?.modelId ?: "gpt-3.5-turbo") }
    var apiKey by remember { mutableStateOf(modelToEdit?.apiKey ?: "") }
    var selectedType by remember { mutableStateOf(modelToEdit?.type ?: AiType.OPENAI) }
    var selectedCapabilities by remember {
        mutableStateOf(
            modelToEdit?.capabilities?.toSet() ?: emptySet()
        )
    }
    var apiKeyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑模型" else "添加模型") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("接口地址") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("模型 ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API 密钥") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (apiKeyVisible) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "切换密钥可见性"
                            )
                        }
                    })

                // AiType Chips (单选)
                Text("模型类型", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiType.entries.forEach { type ->
                        FilterChip(
                            modifier = Modifier.animateContentSize(),
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) },
                            leadingIcon = {
                                if (selectedType != type) return@FilterChip
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Capabilities Chips (多选)
                Text("模型能力", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiCapability.entries.forEach { cap ->
                        val isSelected = selectedCapabilities.contains(cap)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCapabilities =
                                    if (isSelected) selectedCapabilities - cap else selectedCapabilities + cap
                            },
                            label = { Text(cap.name) },
                            leadingIcon = {
                                if (!isSelected) return@FilterChip
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelToEdit != null) viewModel.update(
                        modelToEdit.copy(
                            name = name,
                            baseUrl = baseUrl,
                            modelId = modelId,
                            apiKey = apiKey,
                            type = selectedType,
                            capabilities = selectedCapabilities.toList()
                        )
                    ) else viewModel.add(
                        AiModel(
                            name = name,
                            baseUrl = baseUrl,
                            modelId = modelId,
                            apiKey = apiKey,
                            type = selectedType,
                            capabilities = selectedCapabilities.toList()
                        )
                    )
                    onConfirm()
                },
                enabled = name.isNotBlank() && modelId.isNotBlank() && apiKey.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
