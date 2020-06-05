package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.MessageType

class MessageRes(val id: Long,
                 val type: MessageType,
                 val content: Any,
                 val read: Boolean,
                 @JsonProperty("create_time") val createTime: String)