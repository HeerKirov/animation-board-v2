package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.Min
import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.enums.*
import java.time.LocalDateTime

data class AnimationForm(@Field("title") @NotBlank val title: String,
                         @Field("origin_title") @NotBlank val originTitle: String? = null,
                         @Field("other_title") @NotBlank val otherTitle: String? = null,
                         @Field("publish_type") val publishType: PublishType? = null,
                         @Field("publish_time") val publishTime: String? = null,
                         @Field("duration") @Min(0) val duration: Int? = null,
                         @Field("sum_quantity") @Min(1) val sumQuantity: Int? = null,
                         @Field("published_quantity") @Min(0) val publishedQuantity: Int? = null,
                         @Field("publish_plan") val publishPlan: List<LocalDateTime> = emptyList(),
                         @Field("introduction") val introduction: String? = null,
                         @Field("keyword") val keyword: String? = null,
                         @Field("sex_limit_level") val sexLimitLevel: SexLimitLevel? = null,
                         @Field("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel? = null,
                         @Field("original_work_type") val originalWorkType: OriginalWorkType? = null,
                         @Field("relations") val relations: Map<RelationType, List<Int>> = emptyMap())

class AnimationPartialForm