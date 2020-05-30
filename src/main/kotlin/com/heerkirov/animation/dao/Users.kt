package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.UserSetting
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Users : BaseTable<User>("user") {
    val id by int("id").primaryKey()
    val username by varchar("username")
    val isStaff by boolean("is_staff")
    val setting by json("setting", typeRef<UserSetting>())

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = User(
            id = row[id]!!,
            username = row[username]!!,
            isStaff = row[isStaff]!!,
            setting = row[setting]!!
    )
}