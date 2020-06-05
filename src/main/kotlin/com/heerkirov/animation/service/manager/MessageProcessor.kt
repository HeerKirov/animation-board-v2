package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.Messages
import com.heerkirov.animation.enums.MessageType
import com.heerkirov.animation.model.data.ContentPublish
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toJSONString
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MessageProcessor {
    fun parseContentToObject(type: MessageType, content: String): Any {
        return when(type) {
            MessageType.PUBLISH -> content.parseJSONObject<ContentPublish>()
            MessageType.OTHER -> content
        }
    }

    fun createPublishMessage(ownerId: Int, animationId: Int, oldEpisodes: Int, newEpisodes: Int, now: LocalDateTime): AssignmentsBuilder.(Messages) -> Unit {
        return {
            it.ownerId to ownerId
            it.read to false
            it.type to MessageType.PUBLISH
            it.createTime to now
            it.content to ContentPublish(animationId, oldEpisodes, newEpisodes).toJSONString()
        }
    }
}