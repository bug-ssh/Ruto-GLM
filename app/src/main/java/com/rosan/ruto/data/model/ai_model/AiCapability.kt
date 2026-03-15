package com.rosan.ruto.data.model.ai_model

import androidx.room.TypeConverter

enum class AiCapability(val displayName: String) {
    VISION("图像识别")
}

class AiCapabilityConverter {
    @TypeConverter
    fun fromAiCapabilityList(value: List<AiCapability>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toAiCapabilityList(value: String): List<AiCapability> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { AiCapability.valueOf(it) }
    }
}