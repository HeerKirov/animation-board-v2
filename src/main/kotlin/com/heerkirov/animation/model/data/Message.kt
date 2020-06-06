package com.heerkirov.animation.model.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.MessageType
import java.time.LocalDateTime

data class Message(val id: Long,
                   val ownerId: Int,
                   val type: MessageType,
                   val content: String,
                   val read: Boolean,
                   val createTime: LocalDateTime)

data class ContentPublish(@JsonProperty("animation_id") val animationId: Int,
                          @JsonProperty("old_episodes") val oldEpisodes: Int,
                          @JsonProperty("new_episodes") val newEpisodes: Int)