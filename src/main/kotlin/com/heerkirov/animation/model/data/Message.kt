package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.MessageType
import java.time.LocalDateTime

data class Message(val id: Long,
                   val ownerId: Int,
                   val type: MessageType,
                   val content: String,
                   val read: Boolean,
                   val createTime: LocalDateTime)

data class ContentPublish(val animationId: Int, val oldEpisodes: Int, val newEpisodes: Int)