package com.rosan.ruto.data.model.ai_model

import androidx.room.TypeConverter

enum class AiType(val displayName: String) {
    OPENAI("OpenAI"),
    GEMINI("Gemini")
}

class AiTypeConverter {
    @TypeConverter
    fun fromAiType(value: AiType): String {
        return value.name
    }

    @TypeConverter
    fun toAiType(value: String): AiType {
        return AiType.valueOf(value)
    }
}