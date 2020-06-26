package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.*
import java.time.LocalDateTime

data class Statistic(val id: Long,
                     val ownerId: Int,
                     val type: StatisticType,
                     val key: String?,
                     val content: String,
                     val updateTime: LocalDateTime)

data class OverviewModal(val totalAnimations: Int,
                    val totalEpisodes: Int,
                    val totalDuration: Int,
                    val scoreCounts: Map<Int, Int>,
                    val originalWorkTypeCounts: Map<OriginalWorkType, Int>,
                    val publishTypeCounts: Map<PublishType, Int>,
                    val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                    val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                    val tagCounts: Map<String, Int>)