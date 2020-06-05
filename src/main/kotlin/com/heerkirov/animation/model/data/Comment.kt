package com.heerkirov.animation.model.data

import java.time.LocalDateTime

data class Comment(val id: Int,
                   val ownerId: Int,
                   val animationId: Int,
                   val score: Int?,
                   val title: String?,
                   val article: String?,
                   val createTime: LocalDateTime,
                   val updateTime: LocalDateTime)