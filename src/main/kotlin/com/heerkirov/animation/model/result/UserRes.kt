package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.model.data.UserSetting

data class StatusRes(@JsonProperty("is_login") val isLogin: Boolean,
                     @JsonProperty("is_staff") val isStaff: Boolean?,
                     @JsonProperty("username") val username: String?)

data class SettingRes(@JsonProperty("animation_update_notice") val animationUpdateNotice: Boolean,
                      @JsonProperty("night_time_table") val nightTimeTable: Boolean,
                      @JsonProperty("auto_update_statistics") val autoUpdateStatistics: Boolean)

fun UserSetting.toRes() = SettingRes(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)
