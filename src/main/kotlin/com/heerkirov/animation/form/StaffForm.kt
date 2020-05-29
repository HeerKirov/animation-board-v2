package com.heerkirov.animation.form

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.enums.StaffOccupation
import com.heerkirov.animation.model.Staff
import com.heerkirov.animation.util.toDateTimeString

data class StaffRes(val id: Int,
                    val name: String,
                    @JsonProperty("origin_name") val originName: String?,
                    val remark: String?,
                    val cover: String?,
                    @JsonProperty("is_organization") val isOrganization: Boolean,
                    val occupation: StaffOccupation?,
                    @JsonProperty("create_time") val createTime: String,
                    @JsonProperty("update_time") val updateTime: String)

data class StaffForm(@Field("name") @NotBlank val name: String = "",
                     @Field("origin_name") @NotBlank val originName: String? = null,
                     @Field("remark") @NotBlank val remark: String? = null,
                     @Field("is_organization") val isOrganization: Boolean,
                     @Field("occupation") val occupation: StaffOccupation?)

fun Staff.toRes() = StaffRes(
        id, name, originName, remark, cover, isOrganization, occupation, createTime.toDateTimeString(), updateTime.toDateTimeString()
)