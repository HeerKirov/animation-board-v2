package com.heerkirov.animation.service

import com.heerkirov.animation.model.filter.TagFilter
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.form.TagForm
import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.model.data.User

interface TagService {
    fun list(filter: TagFilter): ListResult<Tag>

    fun get(id: Int): Tag

    fun create(tagForm: TagForm, creator: User): Int

    fun update(id: Int, tagForm: TagForm, updater: User)

    fun delete(id: Int)
}