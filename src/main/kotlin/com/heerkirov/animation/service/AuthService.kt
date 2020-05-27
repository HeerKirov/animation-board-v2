package com.heerkirov.animation.service

import com.heerkirov.animation.service.impl.AuthServiceImpl

interface AuthService {
    fun authenticate(token: String): String

    fun getInfo(username: String): AuthServiceImpl.Info
}