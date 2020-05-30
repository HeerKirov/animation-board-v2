package com.heerkirov.animation.service.impl

import com.heerkirov.animation.model.filter.AnimationFilter
import com.heerkirov.animation.model.form.AnimationForm
import com.heerkirov.animation.model.form.AnimationPartialForm
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.service.AnimationService
import me.liuwj.ktorm.database.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AnimationServiceImpl(@Autowired private val database: Database) : AnimationService {
    override fun list(filter: AnimationFilter, currentUser: User?): ListResult<Animation> {
        TODO("Not yet implemented")
    }

    override fun get(id: Int): Animation {
        TODO("Not yet implemented")
    }

    override fun create(animationForm: AnimationForm, creator: User): Int {
        TODO("Not yet implemented")
    }

    override fun update(id: Int, animationForm: AnimationForm, updater: User) {
        TODO("Not yet implemented")
    }

    override fun partialUpdate(id: Int, animationPartialForm: AnimationPartialForm, updater: User) {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int) {
        TODO("Not yet implemented")
    }

}