package com.rosan.ruto.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rosan.ruto.data.model.conversation.ConversationStatus

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["ai_id"])],
    foreignKeys = [
        ForeignKey(
            entity = AiModel::class,
            parentColumns = ["id"],
            childColumns = ["ai_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConversationModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ai_id")
    val aiId: Long,
    @ColumnInfo(name = "status")
    val status: ConversationStatus = ConversationStatus.COMPLETED,

    @ColumnInfo(name = "display_id")
    val displayId: Int? = null,
    @ColumnInfo(name = "is_glm_phone")
    val isGLMPhone: Boolean = true,


    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
