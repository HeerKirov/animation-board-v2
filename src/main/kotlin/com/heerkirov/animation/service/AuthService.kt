package com.heerkirov.animation.service

interface AuthService {
    fun authenticate(token: String): String
}