package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.*
import com.heerkirov.animation.enums.*

data class AnimationFilter(@Limit val limit: Int?,
                           @Offset val offset: Int?,
                           @Search val search: String?,
                           @Filter("tag") val tag: String?,
                           @Filter("staff") val staff: String?,
                           @Filter("staff_type") val staffType: StaffTypeInAnimation?,
                           @Filter("original_work_type") val originalWorkType: OriginalWorkType?,
                           @Filter("publish_type") val publishType: PublishType?,
                           @Filter("publish_time") val publishTime: String?,
                           @Filter("sex_limit_level") val sexLimitLevel: SexLimitLevel?,
                           @Filter("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel?,
                           @Order(options = ["publish_time", "create_time", "update_time", "sex_limit_level", "violence_limit_level"],
                                   default = "-create_time") val order: List<Pair<Int, String>>)