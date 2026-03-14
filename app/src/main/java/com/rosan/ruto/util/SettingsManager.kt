package com.rosan.ruto.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "ruto_settings"

    // ── AI 模型设置（GuideScreen 使用）────────────────────────────────
    private const val KEY_HOST_URL = "host_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL_ID = "model_id"

    // ── 权限提供者设置（DeviceModule 使用）────────────────────────────
    private const val KEY_PERMISSION_PROVIDER = "permission_provider"
    private const val KEY_TERMINAL_SHELL = "terminal_shell"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── AI 设置方法 ───────────────────────────────────────────────────

    fun saveSettings(context: Context, hostUrl: String, apiKey: String, modelId: String) {
        getPrefs(context).edit {
            putString(KEY_HOST_URL, hostUrl)
            putString(KEY_API_KEY, apiKey)
            putString(KEY_MODEL_ID, modelId)
        }
    }

    fun getHostUrl(context: Context): String {
        return getPrefs(context).getString(KEY_HOST_URL, "https://open.bigmodel.cn/api/paas/v4/") ?: ""
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun getModelId(context: Context): String {
        return getPrefs(context).getString(KEY_MODEL_ID, "autoglm-phone") ?: ""
    }

    fun areSettingsConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hostUrl = prefs.getString(KEY_HOST_URL, null)
        val apiKey = prefs.getString(KEY_API_KEY, null)
        val modelId = prefs.getString(KEY_MODEL_ID, null)
        return !hostUrl.isNullOrBlank() && !apiKey.isNullOrBlank() && !modelId.isNullOrBlank()
    }

    // ── 权限提供者方法 ────────────────────────────────────────────────

    fun savePermissionProvider(
        context: Context,
        provider: PermissionProvider,
        shell: String? = null
    ) {
        getPrefs(context).edit {
            putString(KEY_PERMISSION_PROVIDER, provider.name)
            if (shell != null) {
                putString(KEY_TERMINAL_SHELL, shell)
            }
        }
    }

    fun getPermissionProvider(context: Context): PermissionProvider? {
        val providerName = getPrefs(context).getString(KEY_PERMISSION_PROVIDER, null)
        return providerName?.let { PermissionProvider.valueOf(it) }
    }

    fun getTerminalShell(context: Context): String {
        return getPrefs(context).getString(KEY_TERMINAL_SHELL, "su") ?: "su"
    }

    fun reset(context: Context) {
        getPrefs(context).edit { clear() }
    }
}
