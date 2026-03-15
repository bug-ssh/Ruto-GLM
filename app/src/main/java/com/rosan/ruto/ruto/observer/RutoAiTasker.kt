package com.rosan.ruto.ruto.observer

import android.content.Context
import android.util.Log
import com.rosan.installer.ext.util.toast
import com.rosan.ruto.data.AppDatabase
import com.rosan.ruto.data.model.ConversationModel
import com.rosan.ruto.data.model.MessageModel
import com.rosan.ruto.data.model.conversation.ConversationStatus
import com.rosan.ruto.data.model.message.MessageSource
import com.rosan.ruto.data.model.message.MessageType
import com.rosan.ruto.device.DeviceManager
import com.rosan.ruto.ruto.DefaultRutoRuntime
import com.rosan.ruto.ruto.GLMCommandParser
import com.rosan.ruto.ruto.repo.RutoObserver
import com.rosan.ruto.util.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "RutoAiTasker"
private const val MAX_CONNECT_RETRIES = 3
private const val CONNECT_RETRY_DELAY_MS = 2000L

class RutoAiTasker(
    private val context: Context,
    database: AppDatabase,
    private val deviceManager: DeviceManager
) : RutoObserver {
    private val conversationDao = database.conversations()
    private val messageDao = database.messages()
    private val aisDao = database.ais()
    private val aiJobs = ConcurrentHashMap<Long, Job>()
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onInitialize(scope: CoroutineScope) {
        job = scope.launch {
            conversationDao.observeWhenStatusUpperTime(
                ConversationStatus.COMPLETED, updatedAt = System.currentTimeMillis()
            )
                .scan(emptyList<ConversationModel>() to emptyList<ConversationModel>()) { (oldValue, _), newValue ->
                    newValue to newValue.filter { it !in oldValue }
                }.flatMapConcat { (_, handleValue) ->
                    handleValue.asFlow()
                }.mapNotNull {
                    val displayId = it.displayId ?: return@mapNotNull null
                    displayId to it
                }.collect { (displayId, conversation) ->
                    val convId = conversation.id
                    aiJobs[convId]?.cancelAndJoin()
                    aiJobs[convId] = scope.launch {
                        try {
                            // 确保 Shizuku 已连接，失败则记录日志
                            if (!ensureServiceConnected()) {
                                val msg = "Shizuku 服务连接失败，任务 convId=$convId 已跳过"
                                Log.e(TAG, msg)
                                CrashLogger.logTaskError(context, TAG, msg)
                                conversationDao.updateStatus(convId, ConversationStatus.ERROR)
                                return@launch
                            }
                            processAiRequest(displayId, conversation)
                        } catch (e: Exception) {
                            CrashLogger.logTaskError(
                                context, TAG,
                                "任务执行异常 convId=$convId displayId=$displayId", e
                            )
                            runCatching {
                                conversationDao.updateStatus(convId, ConversationStatus.ERROR)
                            }
                        } finally {
                            if (aiJobs[convId] == coroutineContext[Job]) aiJobs.remove(convId)
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
    }

    private suspend fun ensureServiceConnected(): Boolean {
        repeat(MAX_CONNECT_RETRIES) { attempt ->
            runCatching {
                deviceManager.serviceManager.ensureConnected()
                return true
            }.onFailure { e ->
                Log.w(TAG, "Shizuku 连接尝试 ${attempt + 1}/$MAX_CONNECT_RETRIES 失败: ${e.message}")
                if (attempt < MAX_CONNECT_RETRIES - 1) delay(CONNECT_RETRY_DELAY_MS)
            }
        }
        return false
    }

    private suspend fun processAiRequest(displayId: Int, conversation: ConversationModel) {
        val messages = messageDao.all(conversation.id)
        val lastMessage = messages.lastOrNull()
        if (lastMessage == null) processAiFirstRequest(conversation)
        if (lastMessage == null || (lastMessage.source == MessageSource.USER && lastMessage.type !in arrayOf(
                MessageType.IMAGE_PATH, MessageType.IMAGE_URL
            ))
        ) {
            processAiCaptureRequest(displayId, conversation)
        } else if (lastMessage.source == MessageSource.AI && lastMessage.type == MessageType.TEXT) {
            processAiFunction(displayId, conversation, lastMessage)
        }
    }

    private suspend fun processAiFirstRequest(conversation: ConversationModel) {
        if (!conversation.isGLMPhone) return
        val system = context.assets.open("prompts/glm_phone.txt").bufferedReader().use { it.readText() }
        messageDao.add(MessageModel(conversationId = conversation.id, source = MessageSource.SYSTEM, content = system))
        messageDao.add(MessageModel(conversationId = conversation.id, source = MessageSource.USER, content = conversation.name))
    }

    private suspend fun processAiCaptureRequest(displayId: Int, conversation: ConversationModel) {
        try {
            val bitmap = deviceManager.getDisplayManager().capture(displayId).bitmap
            try {
                messageDao.addImage(conversation.id, bitmap)
            } finally {
                bitmap.recycle()
            }
            conversationDao.updateStatus(conversation.id, ConversationStatus.WAITING)
        } catch (e: Exception) {
            CrashLogger.logTaskError(context, TAG, "截图失败 displayId=$displayId convId=${conversation.id}", e)
            conversationDao.updateStatus(conversation.id, ConversationStatus.ERROR)
        }
    }

    private suspend fun processAiFunction(
        displayId: Int, conversation: ConversationModel, message: MessageModel
    ) {
        val response = message.content
        val parsed = GLMCommandParser.parse(response)
        if (parsed !is GLMCommandParser.Status.Completed) {
            messageDao.addText(conversation.id, "错误，返回未按照要求格式。")
            conversationDao.updateStatus(conversation.id, ConversationStatus.WAITING)
            return
        }
        if (parsed.command.mapping == "finish") {
            context.toast("已完成：" + conversation.name)
            return
        }
        val runtime = DefaultRutoRuntime(deviceManager, displayId)
        try {
            parsed.callFunction(runtime)
            delay(1000)
            processAiCaptureRequest(displayId, conversation)
        } catch (e: Exception) {
            CrashLogger.logTaskError(context, TAG, "执行指令失败 displayId=$displayId convId=${conversation.id}", e)
            messageDao.addText(conversation.id, "错误，执行指令失败：${e.message}")
            conversationDao.updateStatus(conversation.id, ConversationStatus.WAITING)
        }
    }
}
