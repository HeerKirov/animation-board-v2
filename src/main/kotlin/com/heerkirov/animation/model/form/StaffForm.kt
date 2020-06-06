package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.MaxLength
import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.enums.StaffOccupation

data class StaffForm(@Field("name") @NotBlank @MaxLength(64) val name: String,
                     @Field("origin_name") @NotBlank @MaxLength(64) val originName: String? = null,
                     @Field("remark") @NotBlank @MaxLength(64) val remark: String? = null,
                     @Field("is_organization") val isOrganization: Boolean,
                     @Field("occupation") val occupation: StaffOccupation?)
