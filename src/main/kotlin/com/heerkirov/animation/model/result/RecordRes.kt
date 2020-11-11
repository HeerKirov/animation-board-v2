package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.ChaseType
import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.model.data.ActiveEvent
import java.time.LocalDateTime

data class DiaryResult(@JsonProperty("result") val result: List<DiaryItem>,
                       @JsonProperty("night_time_table") val nightTimeTable: Boolean)

data class DiaryItem(@JsonProperty("animation_id") val animationId: Int,
                     @JsonProperty("title") val title: String,
                     @JsonProperty("cover") val cover: String?,
                     @JsonProperty("total_episodes") val totalEpisodes: Int,
                     @JsonProperty("published_episodes") val publishedEpisodes: Int,
                     @JsonProperty("watched_episodes") val watchedEpisodes: Int?,
                     @JsonProperty("next_publish_plan") val nextPublishPlan: String?,
                     @JsonProperty("status") val status: RecordStatus,
                     @JsonProperty("subscription_time") val subscriptionTime: String)

data class TimetableResult(@JsonProperty("result") val result: Map<Int, List<TimetableItem>>,
                           @JsonProperty("night_time_table") val nightTimeTable: Boolean)

data class TimetableItem(@JsonProperty("animation_id") val animationId: Int,
                         @JsonProperty("title") val title: String,
                         @JsonProperty("cover") val cover: String?,
                         @JsonProperty("next_publish_time") val nextPublishTime: String,
                         @JsonProperty("next_publish_episode") val nextPublishEpisode: Int)

data class ActivityRes(@JsonProperty("animation_id") val animationId: Int,
                       @JsonProperty("title") val title: String,
                       @JsonProperty("cover") val cover: String?,
                       @JsonProperty("active_time") val activeTime: String?,
                       @JsonProperty("active_event") val activeEvent: ActiveEvent?,
                       @JsonProperty("progress") val progress: Double?)

data class HistoryRes(@JsonProperty("animation_id") val animationId: Int,
                      @JsonProperty("title") val title: String,
                      @JsonProperty("cover") val cover: String?,
                      @JsonProperty("start_time") val startTime: String?,
                      @JsonProperty("finish_time") val finishTime: String,
                      @JsonProperty("ordinal") val ordinal: Int)

data class ScaleRes(@JsonProperty("animation_id") val animationId: Int,
                    @JsonProperty("title") val title: String,
                    @JsonProperty("cover") val cover: String?,
                    @JsonProperty("ordinal") val ordinal: Int,
                    @JsonProperty("start") val start: String,
                    @JsonProperty("end") val end: String,
                    @JsonProperty("chase_type") val chaseType: ChaseType,
                    @JsonProperty("finished") val finished: Boolean)

data class FindRes(@JsonProperty("animation_id") val animationId: Int,
                   @JsonProperty("title") val title: String,
                   @JsonProperty("cover") val cover: String?,
                   @JsonProperty("total_episodes") val totalEpisodes: Int,
                   @JsonProperty("published_episodes") val publishedEpisodes: Int,
                   @JsonProperty("watched_episodes") val watchedEpisodes: Int?,
                   @JsonProperty("progress") val progress: Double?)

data class RecordDetailRes(@JsonProperty("animation_id") val animationId: Int,
                           @JsonProperty("title") val title: String,
                           @JsonProperty("cover") val cover: String?,
                           @JsonProperty("seen_original") val seenOriginal: Boolean,
                           @JsonProperty("status") val status: RecordStatus,
                           @JsonProperty("in_diary") val inDiary: Boolean,
                           @JsonProperty("total_episodes") val totalEpisodes: Int,
                           @JsonProperty("published_episodes") val publishedEpisodes: Int,
                           @JsonProperty("watched_episodes") val watchedEpisodes: Int,
                           @JsonProperty("progress_count") val progressCount: Int,
                           @JsonProperty("publish_plan") val publishPlan: List<String>,
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