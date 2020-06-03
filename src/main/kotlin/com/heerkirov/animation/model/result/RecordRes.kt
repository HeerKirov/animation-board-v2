package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.RecordStatus

data class RecordDetailRes(@JsonProperty("animation_id") val animationId: Int,
                           @JsonProperty("title") val title: String,
                           @JsonProperty("seen_original") val seenOriginal: Boolean,
                           @JsonProperty("status") val status: RecordStatus,
                           @JsonProperty("in_diary") val inDiary: Boolean,
                           @JsonProperty("total_episodes") val totalEpisodes: Int,
                           @JsonProperty("published_episodes") val publishedEpisodes: Int,
                           @JsonProperty("watched_episodes") val watchedEpisodes: Int,
                           @JsonProperty("progress_count") val progressCount: Int,
                           @JsonProperty("episodes_count") val episodesCount: List<Int>,    //这个数据[i]表示第(i+1)话的播放总次数
                           @JsonProperty("subscription_time") val subscriptionTime: String?,
                           @JsonProperty("finish_time") val finishTime: String?,
                           @JsonProperty("create_time") val createTime: String,
                           @JsonProperty("update_time") val updateTime: String)

data class ProgressRes(@JsonProperty("subscription_time") val subscriptionTime: String?,
                       @JsonProperty("finish_time") val finishTime: String?,
                       @JsonProperty("watched_episodes") val watchedEpisodes: Int)