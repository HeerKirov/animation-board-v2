package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.MessageType

data class MessageRes(val id: Long,
                      val type: MessageType,
                      val content: Any,
                      val read: Boolean,
                      @JsonProperty("create_time") val createTime: String)

data class ContentPublishRes(@JsonProperty("animation_id") val animationId: Int,
                             @JsonProperty("title") val title: String,
                             @JsonProperty("cover") val cover: String?,
                             @JsonProperty("old_episodes") val oldEpisodes: Int,
                             @JsonProperty("new_episodes") val newEpisodes: Int)