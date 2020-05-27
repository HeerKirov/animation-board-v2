package com.heerkirov.animation.model

data class User(val id: Int,
                val username: String,
                val isStaff: Boolean,
                val setting: UserSetting)

data class UserSetting(val animationUpdateNotice: Boolean = false,
                       val nightTimeTable: Boolean = false,
                       val autoUpdateStatistics: Boolean = true)