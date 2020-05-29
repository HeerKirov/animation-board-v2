package com.heerkirov.animation.form

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.aspect.Validate
import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.Form
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.StaffOccupation
import com.heerkirov.animation.exception.BadRequestException
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

@Form
data class StaffForm(@Field("name") val name: String = "",
                     @Field("origin_name") val originName: String? = null,
                     @Field("remark") val remark: String? = null,
                     @Field("is_organization") val isOrganization: Boolean,
                     @Field("occupation") val occupation: StaffOccupation?) {
    @Validate
    fun validate() {
        if(name.isBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Name cannot be empty.")
        if(originName?.isBlank() == true) throw BadRequestException(ErrCode.PARAM_ERROR, "Origin name cannot be blank.")
        if(remark?.isBlank() == true) throw BadRequestException(ErrCode.PARAM_ERROR, "remark cannot be blank.")
    }
}

fun Staff.toRes() = StaffRes(
        id, name, originName, remark, cover, isOrganization, occupation, createTime.toDateTimeString(), updateTime.toDateTimeString()
)