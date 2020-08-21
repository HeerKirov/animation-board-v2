package com.heerkirov.animation.model.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
                         val avgScore: Double?,
                         val scoreCounts: Map<Int, Int>,
                         val originalWorkTypeCounts: Map<OriginalWorkType, Int>,
                         val publishTypeCounts: Map<PublishType, Int>,
                         val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                         val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                         val tagCounts: Map<String, Int>,
                         val sexLimitLevelAvgScores: Map<SexLimitLevel, Double>,
                         val violenceLimitLevelAvgScores: Map<ViolenceLimitLevel, Double>,
                         val tagAvgScores: Map<String, Double>)

data class SeasonOverviewModal(val beginYear: Int?, val beginSeason: Int?, val endYear: Int?, val endSeason: Int?)

data class SeasonModal(val totalAnimations: Int,
                       val maxScore: Int?,
                       val minScore: Int?,
                       val avgScore: Double?,
                       val avgPositivity: Double?,
                       val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                       val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                       val tagCounts: Map<String, Int>,
                       val animations: List<Animation>) {

    data class Animation(val id: Int,
                         val title: String,
                         val cover: String?,
                         val sexLimitLevel: SexLimitLevel?,
                         val violenceLimitLevel: ViolenceLimitLevel?,
                         val subscriptionTime: String,
                         val finishTime: String?,
                         val score: Int?,
                         val positivity: Double?)
}

data class TimelineOverviewModal(val beginYear: Int?, val beginMonth: Int?, val endYear: Int?, val endMonth: Int?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimelineModal(/*compatible: 后续版本可移除nullable标记*/
                         val chaseAnimations: Int?, val chaseEpisodes: Int?, val chaseDuration: Int?,
                         val supplementAnimations: Int?, val supplementEpisodes: Int?, val supplementDuration: Int?,
                         val rewatchedAnimations: Int, val rewatchedEpisodes: Int, val rewatchedDuration: Int,
                         val scatterEpisodes: Int, val scatterDuration: Int,
                         val scoredAnimations: Int, val maxScore: Int?, val minScore: Int?, val sumScore: Int,
                         /*compatible: 后续版本可移除nullable标记*/
                         val scoreCounts: Map<Int, Int>?)

data class HistoryLineModal(val beginYear: Int?,
                            val beginSeason: Int?,
                            val endYear: Int?,
                            val endSeason: Int?,
                            val items: List<Item>) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Item(val year: Int,
                    val season: Int,
                    /*compatible: 后续版本可移除nullable标记*/
                    val chaseAnimations: Int?,
                    val supplementAnimations: Int?,
                    val scoredAnimations: Int,
                    val maxScore: Int?,
                    val minScore: Int?,
                    val sumScore: Int,
                    //compatible: 后续版本可移除nullable标记
                    val scoreCounts: Map<Int, Int>?)
}

data class PeriodOverviewModal(val beginYear: Int?, val endYear: Int?)

data class PeriodModal(val episodeOfWeekdays: Map<Int, Int>,
                       val episodeOfHours: Map<Int, Int>,
                       val dayOfWeekdays: Map<Int, Int>,
                       val dayOfHours: Map<Int, Int>)