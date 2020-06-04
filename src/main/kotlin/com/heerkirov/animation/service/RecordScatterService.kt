package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.ScatterGroupRes
import com.heerkirov.animation.model.result.ScatterItemRes

interface RecordScatterService {
    fun getScatterTable(animationId: Int, user: User): List<ScatterItemRes>

    fun watchScattered(animationId: Int, user: User, episode: Int)

    fun groupScattered(animationId: Int, user: User): ScatterGroupRes
}