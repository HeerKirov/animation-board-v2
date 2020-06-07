package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.data.UserSetting
import java.time.ZoneId

data class SettingForm(@Field("animation_update_notice") val animationUpdateNotice: Boolean,
                       @Field("night_time_table") val nightTimeTable: Boolean,
                       @Field("auto_update_statistics") val autoUpdateStatistics: Boolean,
                       @Field("timezone") val timezone: String)

fun SettingForm.toModel(): UserSetting {
    try {
        ZoneId.of(timezone)
    }catch (e: RuntimeException) {
        throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'timezone' is not a valid timezone.")
    }
    return UserSetting(animationUpdateNotice, nightTimeTable, autoUpdateStatistics, timezone)
}