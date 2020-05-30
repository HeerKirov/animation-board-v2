package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.model.data.UserSetting

data class SettingForm(@Field("animation_update_notice") val animationUpdateNotice: Boolean,
                       @Field("night_time_table") val nightTimeTable: Boolean,
                       @Field("auto_update_statistics") val autoUpdateStatistics: Boolean)

fun SettingForm.toModel() = UserSetting(animationUpdateNotice, nightTimeTable, autoUpdateStatistics)