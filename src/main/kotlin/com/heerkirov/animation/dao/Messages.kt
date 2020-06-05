package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.MessageType
import com.heerkirov.animation.model.data.Message
import com.heerkirov.animation.util.ktorm.StringJacksonConverter
import com.heerkirov.animation.util.ktorm.enum
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Messages : BaseTable<Message>("message") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val type by enum("type", typeRef<MessageType>())
    val content by json("content", StringJacksonConverter())
    val read by boolean("read")
    val createTime by datetime("create_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Message(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            type = row[type]!!,
            content = row[content]!!,
            read = row[read]!!,
            createTime = row[createTime]!!
    )
}