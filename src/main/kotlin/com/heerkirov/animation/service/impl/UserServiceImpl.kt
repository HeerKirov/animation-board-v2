package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.UserSetting
import com.heerkirov.animation.service.AuthService
import com.heerkirov.animation.service.UserService
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
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
            set(it.setting, setting)
            where { it.username eq username }
        } > 0
    }

    private fun createDefaultUser(username: String): User {
        val userSetting = UserSetting()
        val isStaff = authService.getInfo(username).isStaff
        val id = database.insertAndGenerateKey(Users) {
            set(it.username, username)
            set(it.isStaff, isStaff)
            set(it.setting, userSetting)
        }
        return User(id as Int, username, isStaff, userSetting)
    }

}