package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.RecordDetailRes

interface RecordGetterService {
    fun get(animationId: Int, user: User): RecordDetailRes
}