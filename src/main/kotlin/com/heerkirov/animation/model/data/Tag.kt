package com.heerkirov.animation.model.data

import java.time.LocalDateTime

data class Tag(val id: Int,
               val name: String,
               val introduction: String?,
               val group: String?,
               val ordinal: Int,
               val animationCount: Int,
               val createTime: LocalDateTime,
               val updateTime: LocalDateTime,
               val creator: Int,
               val updater: Int)