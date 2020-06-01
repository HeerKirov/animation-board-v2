package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.*
import java.time.LocalDate
import java.time.LocalDateTime

data class Animation(val id: Int,
                     val title: String,
                     val originTitle: String?,
                     val otherTitle: String?,
                     val cover: String?,

                     val publishType: PublishType?,
                     val publishTime: LocalDate?,
                     val duration: Int?,
                     val sumQuantity: Int,
                     val publishedQuantity: Int,
                     val publishedRecord: List<LocalDateTime>,
                     val publishPlan: List<LocalDateTime>,

                     val introduction: String?,
                     val keyword: String?,
                     val sexLimitLevel: SexLimitLevel?,
                     val violenceLimitLevel: ViolenceLimitLevel?,
                     val originalWorkType: OriginalWorkType?,

                     val relations: Map<RelationType, List<Int>>,
                     val relationsTopology: Map<RelationType, List<Int>>,

                     val createTime: LocalDateTime,
                     val updateTime: LocalDateTime,
                     val creator: Int,
                     val updater: Int)