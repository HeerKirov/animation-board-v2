package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.User
import com.heerkirov.animation.model.UserSetting
import com.heerkirov.animation.service.UserService
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(@Autowired private val database: Database) : UserService {
    @Transactional
    override fun get(username: String): User {
        return database.sequenceOf(Users).find { it.username eq username } ?: createDefaultUser(username)
    }

    override fun updateSetting(username: String, setting: UserSetting): User {
        TODO()
    }

    private fun createDefaultUser(username: String): User {
        val id = database.insertAndGenerateKey(Users) {
            it.id to 0
            it.username to username
            it.isStaff to false
            it.setting to UserSetting()
        }
        //TODO 写一个扩展函数快速将data class转换至insert并提取primary key
        return User(id as Int, username, false, UserSetting())
    }

}