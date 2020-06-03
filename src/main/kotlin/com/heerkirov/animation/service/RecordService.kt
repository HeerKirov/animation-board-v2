package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressForm
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.model.result.RecordDetailRes

interface RecordService {
    fun get(animationId: Int, user: User): RecordDetailRes

    fun create(form: RecordCreateForm, user: User)

    fun partialUpdate(animationId: Int, form: RecordPartialForm, user: User)

    fun delete(animationId: Int, user: User)

    fun getProgressList(animationId: Int, user: User): List<ProgressRes>

    fun createProgress(animationId: Int, form: ProgressForm, user: User): ProgressRes
}