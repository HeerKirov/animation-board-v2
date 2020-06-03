package com.heerkirov.animation.model.data

data class User(val id: Int,
                val username: String,
                val isStaff: Boolean,
                val setting: UserSetting)

data class UserSetting(val animationUpdateNotice: Boolean = false,
                       val nightTimeTable: Boolean = false,
                       val autoUpdateStatistics: Boolean = true)
//可选的新选项：
//  + 时区
//  ! 自动沉降
//  ! 自动移出日记