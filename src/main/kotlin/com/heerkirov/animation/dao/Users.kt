package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.UserSetting
import com.heerkirov.animation.util.ktorm.json
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.*

object Users : BaseTable<User>("user") {
    val id = int("id").primaryKey()
    val username = varchar("username")
    val isStaff = boolean("is_staff")
    val setting = json("setting", typeRef<UserSetting>())

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = User(
            id = row[id]!!,
            username = row[username]!!,
            isStaff = row[isStaff]!!,
            setting = row[setting]!!
    )
}