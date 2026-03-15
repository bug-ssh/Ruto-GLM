package com.rosan.ruto.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rosan.ruto.data.dao.AiDao
import com.rosan.ruto.data.dao.ConversationDao
import com.rosan.ruto.data.dao.MessageDao
import com.rosan.ruto.data.model.AiModel
import com.rosan.ruto.data.model.ConversationModel
import com.rosan.ruto.data.model.MessageModel
import com.rosan.ruto.data.model.ai_model.AiCapabilityConverter
import com.rosan.ruto.data.model.ai_model.AiTypeConverter
import com.rosan.ruto.data.model.conversation.ConversationStatusConverter
import com.rosan.ruto.data.model.message.MessageSourceConverters
import com.rosan.ruto.data.model.message.MessageTypeConverter

@Database(
    entities = [ConversationModel::class, MessageModel::class, AiModel::class],
    version = 8
)
@TypeConverters(
    AiTypeConverter::class,
    AiCapabilityConverter::class,

    ConversationStatusConverter::class,

    MessageSourceConverters::class,
    MessageTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun ais(): AiDao
}
