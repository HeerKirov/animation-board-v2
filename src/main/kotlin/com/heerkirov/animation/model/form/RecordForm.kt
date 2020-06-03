package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.Min
import java.time.LocalDateTime

data class RecordCreateForm(@Field("animation_id") val animationId: Int,
                            @Field("create_type") val createType: CreateType,
                            @Field("progress") val progress: List<ProgressForm>? = null) {

    enum class CreateType {
        SUBSCRIBE,      //订阅，并创建首条进度
        SUPPLEMENT,     //补充，并按用户输入补写进度
        RECORD          //记录，不创建任何进度，并随意点击观看
    }
}

data class RecordPartialForm(@Field("seen_original") val seenOriginal: Boolean? = null,
                        @Field("in_diary") val inDiary: Boolean? = null,
                        @Field("watched_episodes") @Min(0) val watchedEpisodes: Int? = null)

data class ScatterForm(@Field("episode") @Min(1) val episode: Int)

data class ProgressCreateForm(@Field("supplement") val supplement: Boolean = false,
                              @Field("start_time") val startTime: LocalDateTime? = null,
                              @Field("finish_time") val finishTime: LocalDateTime? = null,
                              @Field("watched_episodes") @Min(0) val watchedEpisodes: Int? = null)

data class ProgressForm(@Field("start_time") val startTime: LocalDateTime? = null,
                        @Field("finish_time") val finishTime: LocalDateTime? = null,
                        @Field("watched_episodes") @Min(0) val watchedEpisodes: Int? = null)