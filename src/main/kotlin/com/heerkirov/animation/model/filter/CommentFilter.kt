package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.*

data class CommentActivityFilter(@Limit val limit: Int?,
                                 @Offset val offset: Int?,
                                 @Search val search: String?,
                                 @Filter("has_score") val hasScore: Boolean?,
                                 @Filter("has_article") val hasArticle: Boolean?)

data class RankFilter(@Limit val limit: Int?,
                      @Offset val offset: Int?,
                      @Search val search: String?,
                      @Filter("min_score") val minScore: Int?,
                      @Filter("max_score") val maxScore: Int?)

data class CommentFindFilter(@Limit val limit: Int?,
                             @Offset val offset: Int?,
                             @Search val search: String?,
                             @Order(options = ["finish_time", "publish_time", "create_time"], default = "-finish_time") val order: List<Pair<Int, String>>)