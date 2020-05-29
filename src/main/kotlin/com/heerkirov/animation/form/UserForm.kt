package com.heerkirov.animation.form

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.model.UserSetting

data class IsStaffRes(@JsonProperty("is_staff") val isStaff: Boolean)

data class SettingRes(@JsonProperty("animation_update_notice") val animationUpdateNotice: Boolean,
                      @JsonProperty("night_time_table") val nightTimeTable: Boolean,
                      @JsonProperty("auto_update_statistics") val autoUpdateStatistics: Boolean)

data class SettingForm(@Field("animation_update_notice") val animationUpdateNotice: Boolean,
                       @Field("night_time_table") val nightTimeTable: Boolean,
                       @Field("auto_update_statistics") val autoUpdateStatistics: Boolean)

fun UserSetting.toRes() = SettingRes(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)

fun SettingForm.toModel() = UserSetting(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)