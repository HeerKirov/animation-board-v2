package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.MessageType
import com.heerkirov.animation.model.data.Message
import com.heerkirov.animation.util.ktorm.enum
import com.heerkirov.animation.util.ktorm.jsonString
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Messages : BaseTable<Message>("message") {
    val id = long("id").primaryKey()
    val ownerId = int("owner_id")
    val type = enum("type", typeRef<MessageType>())
    val content = jsonString("content")
    val read = boolean("read")
    val createTime = datetime("create_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Message(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            type = row[type]!!,
            content = row[content]!!,
            read = row[read]!!,
            createTime = row[createTime]!!
    )
}