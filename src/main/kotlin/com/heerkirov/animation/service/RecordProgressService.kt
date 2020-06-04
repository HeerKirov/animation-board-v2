package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressCreateForm
import com.heerkirov.animation.model.form.ProgressUpdateForm
import com.heerkirov.animation.model.result.NextRes
import com.heerkirov.animation.model.result.ProgressRes

interface RecordProgressService {
    fun nextEpisode(animationId: Int, user: User): NextRes

    fun getProgressList(animationId: Int, user: User): List<ProgressRes>

    fun createProgress(animationId: Int, form: ProgressCreateForm, user: User): ProgressRes

    fun updateLatestProgress(animationId: Int, form: ProgressUpdateForm, user: User): ProgressRes

    fun deleteProgress(animationId: Int, ordinal: Int, user: User)
}