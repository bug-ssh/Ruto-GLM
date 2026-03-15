package com.rosan.ruto.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10
    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val LOG_DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 安装全局未捕获异常处理器，捕获任务模式及整个 App 的崩溃日志
     */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                writeLog(appContext, "UNCAUGHT", thread.name, throwable)
            }.onFailure { e ->
                Log.e(TAG, "写入崩溃日志失败", e)
            }
            // 继续交给系统默认处理器（弹出 "应用已停止" 对话框）
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i(TAG, "全局崩溃日志已安装")
    }

    /**
     * 手动记录一条任务模式异常日志（不崩溃，只记录）
     */
    fun logTaskError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            writeLog(context.applicationContext, "TASK_ERROR[$tag]", "task-thread", throwable
                ?: RuntimeException(message))
        }.onFailure { e ->
            Log.e(TAG, "写入任务日志失败", e)
        }
        Log.e(TAG, "[$tag] $message", throwable)
    }

    /**
     * 读取所有崩溃日志文件，返回内容列表（最新优先）
     */
    fun readLogs(context: Context): List<CrashLog> {
        val dir = getLogDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                CrashLog(
                    fileName = file.name,
                    timestamp = DATE_FMT.parse(
                        file.nameWithoutExtension.removePrefix("crash_")
                    ) ?: Date(file.lastModified()),
                    content = file.readText()
                )
            } ?: emptyList()
    }

    /**
     * 清空所有崩溃日志
     */
    fun clearLogs(context: Context) {
        getLogDir(context).listFiles()?.forEach { it.delete() }
    }

    // ── 私有方法 ───────────────────────────────────────────────────────────

    private fun writeLog(context: Context, type: String, threadName: String, throwable: Throwable) {
        val dir = getLogDir(context)
        dir.mkdirs()

        // 清理超量文件
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
        if (files.size >= MAX_LOG_FILES) {
            files.take(files.size - MAX_LOG_FILES + 1).forEach { it.delete() }
        }

        val now = Date()
        val file = File(dir, "crash_${DATE_FMT.format(now)}.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val content = buildString {
            appendLine("═══════════════════════════════════════════")
            appendLine("时间：${LOG_DATE_FMT.format(now)}")
            appendLine("类型：$type")
            appendLine("线程：$threadName")
            appendLine("───────────────────────────────────────────")
            appendLine("异常：${throwable::class.java.name}")
            appendLine("信息：${throwable.message}")
            appendLine("───────────────────────────────────────────")
            appendLine("堆栈：")
            appendLine(sw.toString())
            appendLine("═══════════════════════════════════════════")
        }

        file.writeText(content)
        Log.i(TAG, "崩溃日志已写入: ${file.absolutePath}")
    }

    private fun getLogDir(context: Context): File {
        return File(context.filesDir, LOG_DIR)
    }

    data class CrashLog(
        val fileName: String,
        val timestamp: Date,
        val content: String
    )
}
