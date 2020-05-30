package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.enums.StaffOccupation

data class StaffForm(@Field("name") @NotBlank val name: String = "",
                     @Field("origin_name") @NotBlank val originName: String? = null,
                     @Field("remark") @NotBlank val remark: String? = null,
                     @Field("is_organization") val isOrganization: Boolean,
                     @Field("occupation") val occupation: StaffOccupation?)
