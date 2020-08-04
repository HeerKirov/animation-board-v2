package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.StaffOccupation
import com.heerkirov.animation.enums.StaffTypeInAnimation
import com.heerkirov.animation.model.data.Staff
import com.heerkirov.animation.util.toDateTimeString


data class StaffRes(val id: Int,
                    val name: String,
                    @JsonProperty("origin_name") val originName: String?,
                    val remark: String?,
                    val cover: String?,
                    @JsonProperty("is_organization") val isOrganization: Boolean,
                    val occupation: StaffOccupation?,
                    @JsonProperty("animation_count") val animationCount: Int,
                    @JsonProperty("create_time") val createTime: String,
                    @JsonProperty("update_time") val updateTime: String)

data class StaffRelationRes(val id: Int,
                            val name: String,
                            val cover: String?,
                            @JsonProperty("is_organization") val isOrganization: Boolean,
                            val occupation: StaffOccupation?,
                            @JsonProperty("staff_type") val staffType: StaffTypeInAnimation)

fun Staff.toRes() = StaffRes(
        id, name, originName, remark, cover, isOrganization, occupation, animationCount, createTime.toDateTimeString(), updateTime.toDateTimeString()
)