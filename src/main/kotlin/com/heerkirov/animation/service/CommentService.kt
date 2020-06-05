package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.CommentActivityFilter
import com.heerkirov.animation.model.filter.CommentFindFilter
import com.heerkirov.animation.model.filter.RankFilter
import com.heerkirov.animation.model.form.CommentCreateForm
import com.heerkirov.animation.model.form.CommentUpdateForm
import com.heerkirov.animation.model.result.CommentFindRes
import com.heerkirov.animation.model.result.CommentRankRes
import com.heerkirov.animation.model.result.CommentRes
import com.heerkirov.animation.model.result.ListResult

interface CommentService {
    fun activity(filter: CommentActivityFilter, user: User): ListResult<CommentRes>

    fun rank(filter: RankFilter, user: User): ListResult<CommentRankRes>

    fun find(filter: CommentFindFilter, user: User): ListResult<CommentFindRes>

    fun create(form: CommentCreateForm, user: User)

    fun get(animationId: Int, user: User): CommentRes

    fun partialUpdate(animationId: Int, form: CommentUpdateForm, user: User)

    fun delete(animationId: Int, user: User)
}