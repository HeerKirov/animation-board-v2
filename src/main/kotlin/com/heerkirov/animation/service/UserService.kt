package com.heerkirov.animation.service

import com.heerkirov.animation.model.User
import com.heerkirov.animation.model.UserSetting

interface UserService {
    fun get(username: String): User

    fun updateSetting(username: String, setting: UserSetting): User
}