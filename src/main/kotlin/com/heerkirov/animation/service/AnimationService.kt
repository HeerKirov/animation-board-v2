package com.heerkirov.animation.service

import com.heerkirov.animation.model.filter.AnimationFilter
import com.heerkirov.animation.model.form.AnimationForm
import com.heerkirov.animation.model.form.AnimationPartialForm
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.model.data.User

interface AnimationService {
    fun list(filter: AnimationFilter, currentUser: User?): ListResult<Animation>

    fun get(id: Int): Animation

    fun create(animationForm: AnimationForm, creator: User): Int

    fun update(id: Int, animationForm: AnimationForm, updater: User)

    fun partialUpdate(id: Int, animationPartialForm: AnimationPartialForm, updater: User)

    fun delete(id: Int)
}