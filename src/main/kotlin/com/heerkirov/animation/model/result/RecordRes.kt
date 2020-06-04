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
                           @JsonProperty("create_time") val createTime: String,
                           @JsonProperty("update_time") val updateTime: String)

data class ProgressRes(@JsonProperty("ordinal") val ordinal: Int,
                       @JsonProperty("watched_episodes") val watchedEpisodes: Int,
                       @JsonProperty("start_time") val startTime: String?,
                       @JsonProperty("finish_time") val finishTime: String?)

data class NextRes(@JsonProperty("watched_episodes") val watchedEpisodes: Int)

class ScatterItemRes(@JsonProperty("episode") val episode: Int,
                     @JsonProperty("progress_times") val progressTimes: Int,
                     @JsonProperty("scatter_times") val scatterTimes: Int)

class ScatterGroupRes(@JsonProperty("group_to") val groupTo: GroupToType,
                      @JsonProperty("progress_ordinal") val progressOrdinal: Int,
                      @JsonProperty("watched_episodes") val watchedEpisodes: Int,
                      @JsonProperty("append_episodes") val appendEpisodes: Int) {
    enum class GroupToType {
        CURRENT, NEW, NONE
    }
}