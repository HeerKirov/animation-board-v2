package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.OriginalWorkType
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.SexLimitLevel
import com.heerkirov.animation.enums.ViolenceLimitLevel
import com.heerkirov.animation.model.data.OverviewModal
import com.heerkirov.animation.util.toDateTimeString
import java.time.LocalDateTime

data class OverviewRes(@JsonProperty("total_animations") val totalAnimations: Int,
                       @JsonProperty("total_episodes") val totalEpisodes: Int,
                       @JsonProperty("total_duration") val totalDuration: Int,
                       @JsonProperty("score_counts") val scoreCounts: Map<Int, Int>,
                       @JsonProperty("original_work_type_counts") val originalWorkTypeCounts: Map<OriginalWorkType, Int>,
                       @JsonProperty("publish_type_counts") val publishTypeCounts: Map<PublishType, Int>,
                       @JsonProperty("sex_limit_level_counts") val sexLimitLevelCounts: Map<SexLimitLevel, Int>,
                       @JsonProperty("violence_limit_level_counts") val violenceLimitLevelCounts: Map<ViolenceLimitLevel, Int>,
                       @JsonProperty("tag_counts") val tagCounts: Map<String, Int>,
                       @JsonProperty("update_time") val updateTime: String)

fun OverviewModal.toResWith(updateTime: LocalDateTime): OverviewRes {
    return OverviewRes(totalAnimations, totalEpisodes, totalDuration, scoreCounts, originalWorkTypeCounts, publishTypeCounts, sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts, updateTime.toDateTimeString())
}