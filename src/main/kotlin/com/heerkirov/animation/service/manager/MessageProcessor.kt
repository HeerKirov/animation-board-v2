package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Messages
import com.heerkirov.animation.enums.MessageType
import com.heerkirov.animation.model.data.ContentPublish
import com.heerkirov.animation.model.result.ContentPublishRes
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toJSONString
import com.heerkirov.animation.util.ktorm.dsl.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MessageProcessor(@Autowired private val database: Database) {
    /**
     * 将消息内容转换为可以返回给用户的格式。
     */
    fun parseContentToObject(type: MessageType, content: String): Any {
        return when(type) {
            MessageType.PUBLISH -> {
                val parsedContent = content.parseJSONObject<ContentPublish>()
                val rowSet = database.from(Animations).select(Animations.title, Animations.cover).where { Animations.id eq parsedContent.animationId }.firstOrNull()
                ContentPublishRes(parsedContent.animationId, rowSet?.get(Animations.title) ?: "", rowSet?.get(Animations.cover), parsedContent.oldEpisodes, parsedContent.newEpisodes)
            }
            MessageType.OTHER -> content
        }
    }

    /**
     * 创建一条publish消息。
     * @return 用于数据库创建的闭包
     */
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