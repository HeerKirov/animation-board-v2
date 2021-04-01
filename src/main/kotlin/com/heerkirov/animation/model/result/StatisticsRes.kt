package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.OriginalWorkType
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.SexLimitLevel
import com.heerkirov.animation.enums.ViolenceLimitLevel
import com.heerkirov.animation.model.data.*
import com.heerkirov.animation.util.toDateTimeString
import org.ktorm.dsl.avg
import java.time.LocalDateTime

data class OverviewRes(@JsonProperty("total_animations") val totalAnimations: Int,
                       @JsonProperty("total_episodes") val totalEpisodes: Int,
                       @JsonProperty("total_duration") val totalDuration: Int,
                       @JsonProperty("avg_score") val avgScore: Double?,
                       @JsonProperty("score_counts") val scoreCounts: Map<Int, Int>,
                       @JsonProperty("original_work_type_counts") val originalWorkTypeCounts: Map<OriginalWorkType, Int>,
                       @JsonProperty("publish_type_counts") val publishTypeCounts: Map<PublishType, Int>,
                       @JsonProperty("sex_limit_level_counts") val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                       @JsonProperty("violence_limit_level_counts") val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                       @JsonProperty("tag_counts") val tagCounts: Map<String, Int>,
                       @JsonProperty("sex_limit_level_avg_scores") val sexLimitLevelAvgScores: Map<SexLimitLevel, Double>,
                       @JsonProperty("violence_limit_level_avg_scores") val violenceLimitLevelAvgScores: Map<ViolenceLimitLevel, Double>,
                       @JsonProperty("tag_avg_scores") val tagAvgScores: Map<String, Double>,
                       @JsonProperty("update_time") val updateTime: String)

fun OverviewModal.toResWith(updateTime: LocalDateTime): OverviewRes {
    return OverviewRes(totalAnimations, totalEpisodes, totalDuration, avgScore,
            scoreCounts, originalWorkTypeCounts, publishTypeCounts,
            sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts,
            sexLimitLevelAvgScores, violenceLimitLevelAvgScores, tagAvgScores,
            updateTime.toDateTimeString())
}

data class SeasonOverviewRes(@JsonProperty("begin_year") val beginYear: Int?,
                             @JsonProperty("begin_season") val beginSeason: Int?,
                             @JsonProperty("end_year") val endYear: Int?,
                             @JsonProperty("end_season") val endSeason: Int?,
                             @JsonProperty("update_time") val updateTime: String?)

fun SeasonOverviewModal.toResWith(updateTime: LocalDateTime): SeasonOverviewRes {
    return SeasonOverviewRes(beginYear, beginSeason, endYear, endSeason, updateTime.toDateTimeString())
}

data class SeasonLineRes(@JsonProperty("items") val items: List<Item>,
                         @JsonProperty("update_time") val updateTime: String?) {
    data class Item(@JsonProperty("year") val year: Int,
                    @JsonProperty("season") val season: Int,
                    @JsonProperty("total_animations") val totalAnimations: Int,
                    @JsonProperty("max_score") val maxScore: Int?,
                    @JsonProperty("min_score") val minScore: Int?,
                    @JsonProperty("avg_score") val avgScore: Double?,
                    @JsonProperty("avg_positivity") val avgPositivity: Double?)
}

data class SeasonRes(@JsonProperty("total_animations") val totalAnimations: Int,
                     @JsonProperty("max_score") val maxScore: Int?,
                     @JsonProperty("min_score") val minScore: Int?,
                     @JsonProperty("avg_score") val avgScore: Double?,
                     @JsonProperty("avg_positivity") val avgPositivity: Double?,
                     @JsonProperty("sex_limit_level_counts") val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                     @JsonProperty("violence_limit_level_counts") val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                     @JsonProperty("tag_counts") val tagCounts: Map<String, Int>,
                     @JsonProperty("animations") val animations: List<Animation>,
                     @JsonProperty("update_time") val updateTime: String) {
    data class Animation(@JsonProperty("id") val id: Int,
                         @JsonProperty("title") val title: String,
                         @JsonProperty("cover") val cover: String?,
                         @JsonProperty("sex_limit_level") val sexLimitLevel: SexLimitLevel?,
                         @JsonProperty("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel?,
                         @JsonProperty("subscription_time") val subscriptionTime: String,
                         @JsonProperty("finish_time") val finishTime: String?,
                         @JsonProperty("score") val score: Int?,
                         @JsonProperty("positivity") val positivity: Double?)
}

fun SeasonModal.toResWith(updateTime: LocalDateTime): SeasonRes {
    return SeasonRes(totalAnimations, maxScore, minScore, avgScore, avgPositivity, sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts,
            animations.map { SeasonRes.Animation(it.id, it.title, it.cover, it.sexLimitLevel, it.violenceLimitLevel,
                    it.subscriptionTime, it.finishTime, it.score, it.positivity) },
            updateTime.toDateTimeString())
}

data class TimelineOverviewRes(@JsonProperty("begin_year") val beginYear: Int?,
                               @JsonProperty("begin_month") val beginMonth: Int?,
                               @JsonProperty("end_year") val endYear: Int?,
                               @JsonProperty("end_month") val endMonth: Int?,
                               @JsonProperty("update_time") val updateTime: String?)

data class TimelineRes(@JsonProperty("items") val items: List<Item>,
                       @JsonProperty("update_time") val updateTime: String?) {
    data class Item(@JsonProperty("time") val time: String,
                    @JsonProperty("chase_animations") val chaseAnimations: Int,
                    @JsonProperty("chase_episodes") val chaseEpisodes: Int,
                    @JsonProperty("chase_duration") val chaseDuration: Int,
                    @JsonProperty("supplement_animations") val supplementAnimations: Int,
                    @JsonProperty("supplement_episodes") val supplementEpisodes: Int,
                    @JsonProperty("supplement_duration") val supplementDuration: Int,
                    @JsonProperty("rewatched_animations") val rewatchedAnimations: Int,
                    @JsonProperty("rewatched_episodes") val rewatchedEpisodes: Int,
                    @JsonProperty("rewatched_duration") val rewatchedDuration: Int,
                    @JsonProperty("scatter_episodes") val scatterEpisodes: Int,
                    @JsonProperty("scatter_duration") val scatterDuration: Int,
                    @JsonProperty("max_score") val maxScore: Int?,
                    @JsonProperty("min_score") val minScore: Int?,
                    @JsonProperty("avg_score") val avgScore: Double?,
                    @JsonProperty("score_counts") val scoreCounts: Map<Int, Int>)
}

data class HistoryLineRes(@JsonProperty("items") val items: List<Item>,
                          @JsonProperty("update_time") val updateTime: String) {
    data class Item(@JsonProperty("time") val time: String,
                    @JsonProperty("chase_animations") val chaseAnimations: Int,
                    @JsonProperty("supplement_animations") val supplementAnimations: Int,
                    @JsonProperty("max_score") val maxScore: Int?,
                    @JsonProperty("min_score") val minScore: Int?,
                    @JsonProperty("avg_score") val avgScore: Double?,
                    @JsonProperty("score_counts") val scoreCounts: Map<Int, Int>)
}

data class PeriodOverviewRes(@JsonProperty("begin_year") val beginYear: Int?,
                             @JsonProperty("end_year") val endYear: Int?,
                             @JsonProperty("update_time") val updateTime: String?)

fun PeriodOverviewModal.toResWith(updateTime: LocalDateTime): PeriodOverviewRes {
    return PeriodOverviewRes(beginYear, endYear, updateTime.toDateTimeString())
}

data class PeriodRes(@JsonProperty("episode_of_hours") val episodeOfHours: Map<Int, Int>,
                     @JsonProperty("episode_of_weekdays") val episodeOfWeekdays: Map<Int, Int>,
                     @JsonProperty("day_of_hours") val dayOfHours: Map<Int, Int>,
                     @JsonProperty("day_of_weekdays") val dayOfWeekdays: Map<Int, Int>,
                     @JsonProperty("update_time") val updateTime: String)

fun PeriodModal.toResWith(updateTime: LocalDateTime): PeriodRes {
    return PeriodRes(episodeOfHours, episodeOfWeekdays, dayOfHours, dayOfWeekdays, updateTime.toDateTimeString())
}