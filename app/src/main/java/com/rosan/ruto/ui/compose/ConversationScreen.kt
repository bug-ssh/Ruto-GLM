package com.rosan.ruto.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.rosan.ruto.data.model.MessageModel
import com.rosan.ruto.data.model.conversation.ConversationStatus
import com.rosan.ruto.data.model.message.MessageSource
import com.rosan.ruto.data.model.message.MessageType
import com.rosan.ruto.ui.compose.theme.only
import com.rosan.ruto.ui.compose.theme.plus
import com.rosan.ruto.ui.viewmodel.ConversationViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(navController: NavController, insets: WindowInsets, conversationId: Long) {
    val viewModel: ConversationViewModel = koinViewModel { parametersOf(conversationId) }
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val status by viewModel.status.collectAsState()
    val isRunning = status == ConversationStatus.RUNNING
    val textState = remember { mutableStateOf("") }
    val (selectedMessages, setSelectedMessages) = remember { mutableStateOf(emptySet<Long>()) }
    val isInSelectionMode = selectedMessages.isNotEmpty()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isAtBottom = !listState.canScrollForward

    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty() && !isAtBottom) {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isInSelectionMode) Text("${selectedMessages.size} selected")
                    else Text("对话")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isInSelectionMode) setSelectedMessages(emptySet())
                        else navController.popBackStack()
                    }) {
                        Icon(
                            if (isInSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        IconButton(onClick = {
                            viewModel.remove(selectedMessages.toList())
                            setSelectedMessages(emptySet())
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            )
        },
        contentWindowInsets = insets
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(
                start = 8.dp, end = 8.dp, top = 8.dp,
                bottom = bottomBarHeight + 8.dp
            ) + padding.only(WindowInsetsSides.Vertical),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                messages.reversed(),
                key = { _, message -> message.id }) { index, message ->
                val isSelected = selectedMessages.contains(message.id)
                MessageBubble(
                    isRunning = isRunning,
                    isLastMessage = index == 0,
                    message = message,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(300),
                            placementSpec = spring(stiffness = Spring.StiffnessLow)
                        ),
                    padding = padding.only(WindowInsetsSides.Horizontal),
                    isSelected = isSelected,
                    isInSelectionMode = isInSelectionMode,
                    onToggleSelection = {
                        setSelectedMessages(
                            if (isSelected) selectedMessages - message.id
                            else selectedMessages + message.id
                        )
                    },
                    onEnterSelectionMode = { setSelectedMessages(setOf(message.id)) },
                    onDelete = { viewModel.remove(listOf(message.id)) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        bottomBarHeight = with(density) { it.size.height.toDp() }
                    }
                    .animateContentSize()
            ) {
                ConversationFloatingBar(conversationId, viewModel, textState, isRunning)
            }
        }
    }
}

@Composable
fun ConversationFloatingBar(
    conversationId: Long,
    viewModel: ConversationViewModel,
    textState: MutableState<String>,
    isRunning: Boolean,
) {
    var text by textState
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                viewModel.addImage(stream)
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入消息…") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 6
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                    }
                }

                AnimatedContent(isRunning, label = "") { running ->
                    IconButton(onClick = {
                        if (running) viewModel.stop()
                        else if (text.isNotBlank()) {
                            viewModel.add(
                                MessageModel(
                                    conversationId = conversationId,
                                    source = MessageSource.USER,
                                    type = MessageType.TEXT,
                                    content = text
                                )
                            )
                            text = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (running) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (text.isNotBlank() || running) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    isRunning: Boolean,
    isLastMessage: Boolean,
    message: MessageModel,
    modifier: Modifier,
    padding: PaddingValues,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val isUser = message.source == MessageSource.USER ||
            message.source == MessageSource.SYSTEM

    val bubbleShape =
        if (isUser) MaterialTheme.shapes.large.copy(bottomEnd = CornerSize(0.dp))
        else MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp))

    val horizontalPadding = if (isUser) PaddingValues(start = 24.dp) else PaddingValues(end = 24.dp)

    val bubbleColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
        else if (message.type == MessageType.ERROR) MaterialTheme.colorScheme.errorContainer
        else if (isUser) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.inversePrimary
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isInSelectionMode) onToggleSelection()
                    else if (message.content.length > 150) isExpanded = !isExpanded
                },
                onLongClick = { if (!isInSelectionMode) showMenu = true },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = formatTimestamp(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(if (isUser) Alignment.End else Alignment.Start)
                .padding(horizontalPadding),
            color = LocalContentColor.current.copy(alpha = 0.5f)
        )

        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontalPadding),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = bubbleShape,
                    color = bubbleColor,
                    tonalElevation = 1.dp
                ) {
                    when (message.type) {
                        MessageType.TEXT, MessageType.ERROR -> {
                            Column(modifier = Modifier.animateContentSize()) {
                                Box(
                                    modifier = Modifier
                                        .clip(bubbleShape)
                                ) {
                                    SelectionContainer {
                                        RichText(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .then(
                                                    if (!isExpanded && message.content.length > 150) Modifier.heightIn(
                                                        max = 200.dp
                                                    )
                                                    else Modifier
                                                )
                                        ) {
                                            val content =
                                                if (!isExpanded && message.content.length > 150) message.content.take(
                                                    300
                                                )
                                                else message.content
                                            Markdown(content)
                                        }
                                    }
                                    if (!isExpanded && message.content.length > 150) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            bubbleColor.copy(alpha = 0.7f)
                                                        ),
                                                        startY = 300f
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            AsyncImage(
                                model = message.content, contentDescription = null,
                                modifier = Modifier
                                    .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                                    .border(4.dp, color = bubbleColor, shape = bubbleShape)
                            )
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { onDelete(); showMenu = false })
                DropdownMenuItem(
                    text = { Text("选择") },
                    onClick = { onEnterSelectionMode(); showMenu = false })
            }
            AnimatedVisibility(
                visible = isInSelectionMode,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onToggleSelection,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (!isUser && isLastMessage && isRunning) {
            TypingIndicator(color = LocalContentColor.current)
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msg = Calendar.getInstance().apply { timeInMillis = timestamp }
    val locale = Locale.getDefault()

    val pattern = when {
        now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR) -> {
            "HH:mm:ss"
        }


        now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) -> {
            "MM-dd HH:mm:ss"
        }

        else -> {
            "yyyy-MM-dd HH:mm:ss"
        }
    }

    return SimpleDateFormat(pattern, locale).format(Date(timestamp))
}

@Composable
fun TypingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "typing")

    // 创建三个点的动画值，每个点都有延迟
    val alphas = (0..2).map { index ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 200) // 错开时间
            ),
            label = "alpha_$index"
        ).value
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        alphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer(alpha = alpha)
                    .background(color, CircleShape)
            )
        }
    }
}