package com.heerkirov.animation.service

import com.heerkirov.animation.form.TagForm
import com.heerkirov.animation.model.Tag
import com.heerkirov.animation.model.User

interface TagService {
    fun list(): List<Tag>

    fun get(id: Int): Tag

    fun create(tagForm: TagForm, creator: User): Int

    fun update(id: Int, tagForm: TagForm, updater: User)

    fun delete(id: Int)
}