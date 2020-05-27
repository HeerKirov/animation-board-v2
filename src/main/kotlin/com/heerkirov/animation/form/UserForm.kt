package com.heerkirov.animation.form

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.model.UserSetting

data class StaffRes(@JsonProperty("is_staff") val isStaff: Boolean)

data class SettingForm(@JsonProperty("animation_update_notice") val animationUpdateNotice: Boolean,
                       @JsonProperty("night_time_table") val nightTimeTable: Boolean,
                       @JsonProperty("auto_update_statistics") val autoUpdateStatistics: Boolean)

fun UserSetting.toForm() = SettingForm(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)

fun SettingForm.toModel() = UserSetting(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)