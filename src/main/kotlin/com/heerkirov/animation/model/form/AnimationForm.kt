package com.heerkirov.animation.model.form

import com.heerkirov.animation.enums.*
import java.time.LocalDateTime

data class AnimationForm(val title: String,
                         val originTitle: String? = null,
                         val otherTitle: String? = null,
                         val publishType: PublishType? = null,
                         val publishTime: String? = null,
                         val duration: Int? = null,
                         val sumQuantity: Int? = null,
                         val publishedQuantity: Int? = null,
                         val publishPlan: List<LocalDateTime> = emptyList(),
                         val introduction: String? = null,
                         val keyword: String? = null,
                         val sexLimitLevel: SexLimitLevel? = null,
                         val violenceLimitLevel: ViolenceLimitLevel? = null,
                         val originalWorkType: OriginalWorkType? = null,
                         val relations: Map<RelationType, List<Int>> = emptyMap())

class AnimationPartialForm