package com.heerkirov.animation.service

import com.heerkirov.animation.model.filter.TagFilter
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.form.TagUpdateForm
import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.TagCreateForm
import com.heerkirov.animation.model.form.TagPartialForm

interface TagService {
    fun list(filter: TagFilter): ListResult<Tag>

    fun get(id: Int): Tag

    fun create(tagForm: TagCreateForm, creator: User): Int

    fun update(id: Int, tagForm: TagUpdateForm, updater: User)

    fun partialUpdate(id: Int, tagForm: TagPartialForm, updater: User)

    fun delete(id: Int)
}