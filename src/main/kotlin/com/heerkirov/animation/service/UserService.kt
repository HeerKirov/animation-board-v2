package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.UserSetting

interface UserService {
    fun get(username: String): User

    fun updateSetting(username: String, setting: UserSetting): Boolean
}