package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.MaxLength
import com.heerkirov.animation.aspect.validation.Range

data class CommentCreateForm(@Field("animation_id") val animationId: Int,
                             @Field("score") @Range(min = 1, max = 10) val score: Int? = null,
                             @Field("article_title") @MaxLength(128) val articleTitle: String? = null,
                             @Field("article") val article: String? = null)

data class CommentUpdateForm(@Field("score") @Range(min = 1, max = 10) val score: Int? = null,
                             @Field("article_title") @MaxLength(128) val articleTitle: String? = null,
                             @Field("article") val article: String? = null)