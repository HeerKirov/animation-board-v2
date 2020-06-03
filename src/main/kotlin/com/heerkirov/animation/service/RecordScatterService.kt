package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User

interface RecordScatterService {
    fun watchScattered(animationId: Int, user: User, episode: Int)
}