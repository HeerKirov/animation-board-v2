package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.model.data.UserSetting

data class IsStaffRes(@JsonProperty("is_staff") val isStaff: Boolean)

data class SettingRes(@JsonProperty("animation_update_notice") val animationUpdateNotice: Boolean,
                      @JsonProperty("night_time_table") val nightTimeTable: Boolean,
                      @JsonProperty("auto_update_statistics") val autoUpdateStatistics: Boolean)

fun UserSetting.toRes() = SettingRes(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)
