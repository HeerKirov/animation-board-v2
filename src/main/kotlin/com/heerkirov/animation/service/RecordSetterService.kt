package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm

interface RecordSetterService {
    fun create(form: RecordCreateForm, user: User)

    fun partialUpdate(animationId: Int, form: RecordPartialForm, user: User)

    fun delete(animationId: Int, user: User)
}