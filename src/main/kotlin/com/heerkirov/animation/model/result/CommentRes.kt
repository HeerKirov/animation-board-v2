package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty

data class CommentRankRes(@JsonProperty("animation_id") val animationId: Int,
                          @JsonProperty("title") val title: String,
                          @JsonProperty("cover") val cover: String?,
                          @JsonProperty("score") val score: Int?)

data class CommentFindRes(@JsonProperty("animation_id") val animationId: Int,
                          @JsonProperty("title") val title: String,
                          @JsonProperty("cover") val cover: String?,
                          @JsonProperty("finish_time") val finishTime: String,
                          @JsonProperty("publish_time") val publishTime: String?,
                          @JsonProperty("create_time") val createTime: String)

data class CommentRes(@JsonProperty("animation_id") val animationId: Int,
                      @JsonProperty("title") val title: String,
                      @JsonProperty("cover") val cover: String?,
                      @JsonProperty("score") val score: Int?,
                      @JsonProperty("article_title") val articleTitle: String?,
                      @JsonProperty("article") val article: String?,
                      @JsonProperty("create_time") val createTime: String,
                      @JsonProperty("update_time") val updateTime: String)