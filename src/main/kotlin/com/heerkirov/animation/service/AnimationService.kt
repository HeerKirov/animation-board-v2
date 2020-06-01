package com.heerkirov.animation.service

import com.heerkirov.animation.model.filter.AnimationFilter
import com.heerkirov.animation.model.form.AnimationForm
import com.heerkirov.animation.model.form.AnimationPartialForm
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.AnimationResult

interface AnimationService {
    fun list(filter: AnimationFilter, currentUser: User?): ListResult<Animation>

    fun get(id: Int): AnimationResult

    fun create(animationForm: AnimationForm, creator: User): Int

    fun partialUpdate(id: Int, animationPartialForm: AnimationPartialForm, updater: User)

    fun delete(id: Int)
}