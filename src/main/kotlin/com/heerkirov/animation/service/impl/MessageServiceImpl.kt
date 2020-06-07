package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Messages
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.MessageFilter
import com.heerkirov.animation.model.form.MarkAsReadForm
import com.heerkirov.animation.model.result.MessageRes
import com.heerkirov.animation.service.MessageService
import com.heerkirov.animation.service.manager.MessageProcessor
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MessageServiceImpl(@Autowired private val database: Database,
                         @Autowired private val messageProcessor: MessageProcessor) : MessageService {
    override fun messages(filter: MessageFilter, user: User): List<MessageRes> {
        return getQuery(filter, user).asSequence()
                .map { Messages.createEntity(it) }
                .map { MessageRes(it.id, it.type, messageProcessor.parseContentToObject(it.type, it.content), it.read, it.createTime.toDateTimeString()) }
                .toList()
    }

    override fun messageCount(filter: MessageFilter, user: User): Int {
        return getQuery(filter, user).totalRecords
    }

    override fun markAsRead(form: MarkAsReadForm, user: User): Int {
        val messages = database.from(Messages).select()
                .where { (Messages.id inList form.ids) and (Messages.ownerId eq user.id) and (Messages.read eq false) }
                .map { Messages.createEntity(it) }
        database.batchUpdate(Messages) {
            for (message in messages) {
                item {
                    where { it.id eq message.id }
                    it.read to true
                }
            }
        }
        return messages.size
    }

    private fun getQuery(filter: MessageFilter, user: User): Query {
        return database.from(Messages).select().whereWithConditions {
            it += Messages.ownerId eq user.id
            if(filter.unread != null) it += Messages.read eq !filter.unread
            if(filter.from != null) it += Messages.id greater filter.from
        }.orderBy(Messages.createTime.desc())
    }
}