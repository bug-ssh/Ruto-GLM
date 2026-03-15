package com.rosan.ruto.ui.compose.screen_preview

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext

enum class AppSortBy(val label: String) {
    AppName("应用名称"), PackageName("包名"), InstallTime("安装时间")
}

// 新增过滤枚举
enum class AppFilter(val label: String) {
    All("全部"),
    User("用户应用"),
    System("系统应用")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val focusRequester = remember { FocusRequester() }

    val apps by produceState<List<ApplicationInfo>?>(initialValue = null) {
        value = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0)
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
        }
    }

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(AppSortBy.AppName) }
    var sortDescending by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(AppFilter.All) } // 过滤状态

    val filteredAndSortedApps = remember(apps, searchText, sortBy, sortDescending, currentFilter) {
        apps?.filter { app ->
            // 1. 过滤类型逻辑
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val typeMatch = when (currentFilter) {
                AppFilter.All -> true
                AppFilter.User -> !isSystem
                AppFilter.System -> isSystem
            }
            // 2. 搜索逻辑
            val appName = app.loadLabel(pm).toString()
            val searchMatch = appName.contains(searchText, ignoreCase = true) ||
                    app.packageName.contains(searchText, ignoreCase = true)

            typeMatch && searchMatch
        }?.sortedWith(
            compareBy<ApplicationInfo> {
                when (sortBy) {
                    AppSortBy.AppName -> it.loadLabel(pm).toString()
                    AppSortBy.PackageName -> it.packageName
                    AppSortBy.InstallTime -> try {
                        pm.getPackageInfo(it.packageName, 0).firstInstallTime
                    } catch (e: Exception) {
                        0L
                    }
                }
            }.let { if (sortDescending) it.reversed() else it }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            SearchBarHeader(
                isSearching = isSearching,
                searchText = searchText,
                focusRequester = focusRequester,
                onSearchToggle = { isSearching = it },
                onTextChange = { searchText = it }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 第一行：排序控制
                SortControl(sortBy, sortDescending, { sortBy = it }, { sortDescending = it })

                // 第二行：新增过滤 Chip 栏
                FilterChips(currentFilter) { currentFilter = it }

                Box(modifier = Modifier.heightIn(min = 200.dp, max = 450.dp)) {
                    AnimatedContent(
                        targetState = filteredAndSortedApps,
                        label = "ListAnimation",
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
                    ) { applist ->
                        when {
                            applist == null -> LoadingState()
                            applist.isEmpty() -> EmptyState(isSearching || currentFilter != AppFilter.All)
                            else -> AppLazyList(applist, searchText, onAppSelected)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SearchBarHeader(
    isSearching: Boolean,
    searchText: String,
    focusRequester: FocusRequester,
    onSearchToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit
) {
    AnimatedContent(
        targetState = isSearching,
        transitionSpec = {
            (fadeIn() + expandHorizontally(expandFrom = Alignment.End))
                .togetherWith(fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End))
        },
        label = "TitleAnimation"
    ) { searching ->
        if (!searching) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("选择应用", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Default.Search, "搜索")
                }
            }
        } else {
            OutlinedTextField(
                value = searchText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("搜索应用…") },
                trailingIcon = {
                    IconButton(onClick = { onSearchToggle(false); onTextChange("") }) {
                        Icon(Icons.Default.Close, "清除")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun FilterChips(selected: AppFilter, onSelected: (AppFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
                leadingIcon = if (selected == filter) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null,
                shape = CircleShape
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppLazyList(
    apps: List<ApplicationInfo>,
    query: String,
    onAppSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(apps, key = { it.packageName }) { appInfo ->
            AppInfoRow(
                appInfo = appInfo,
                query = query,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAppSelected(appInfo.packageName) }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isFiltered) Icons.Default.FilterListOff else Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text("未找到相关应用", color = MaterialTheme.colorScheme.outline)
    }
}


@Composable
private fun AppInfoRow(
    appInfo: ApplicationInfo,
    query: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = remember(appInfo.packageName) { appInfo.loadIcon(pm) }
        val bitmap = remember(icon) {
            val bmp = Bitmap.createBitmap(
                icon.intrinsicWidth.coerceAtLeast(1),
                icon.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)
            bmp
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val appLabel = appInfo.loadLabel(pm).toString()
            HighlightedText(
                text = appLabel,
                query = query,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle
) {
    val annotatedString = buildAnnotatedString {
        val startIndex = text.indexOf(query, ignoreCase = true)
        if (query.isNotEmpty() && startIndex != -1) {
            append(text.substring(0, startIndex))
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(startIndex, startIndex + query.length))
            }
            append(text.substring(startIndex + query.length))
        } else {
            append(text)
        }
    }
    Text(text = annotatedString, style = style)
}

@Composable
private fun SortControl(
    sortBy: AppSortBy,
    isDescending: Boolean,
    onSortByChange: (AppSortBy) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            TextButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    modifier = Modifier.size(18.dp),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "排序：${sortBy.label}", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppSortBy.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(it.label) },
                        onClick = {
                            onSortByChange(it)
                            expanded = false
                        }
                    )
                }
            }
        }

        IconButton(onClick = { onSortDescendingChange(!isDescending) }) {
            val rotation by animateFloatAsState(if (isDescending) 180f else 0f, label = "Rotate")
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}