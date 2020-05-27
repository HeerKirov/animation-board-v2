package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.User
import com.heerkirov.animation.model.UserSetting
import com.heerkirov.animation.service.AuthService
import com.heerkirov.animation.service.UserService
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(@Autowired private val authService: AuthService,
                      @Autowired private val database: Database) : UserService {
    @Transactional
    override fun get(username: String): User {
        return database.sequenceOf(Users).find { it.username eq username } ?: createDefaultUser(username)
    }

    override fun updateSetting(username: String, setting: UserSetting): Boolean {
        return database.update(Users) {
            it.setting to setting
            where { it.username eq username }
        } > 0
    }

    private fun createDefaultUser(username: String): User {
        val userSetting = UserSetting()
        val isStaff = authService.getInfo(username).isStaff
        val id = database.insertAndGenerateKey(Users) {
            it.username to username
            it.isStaff to isStaff
            it.setting to userSetting
        }
        //TODO 写一个扩展函数快速将data class转换至insert并提取primary key
        return User(id as Int, username, isStaff, userSetting)
    }

}