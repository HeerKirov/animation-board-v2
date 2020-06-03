package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressForm
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/personal/records")
class RecordController(@Autowired private val recordService: RecordService) {
    @Authorization
    @GetMapping("/{animationId}")
    fun retrieve(@UserIdentity user: User, @PathVariable animationId: Int): RecordDetailRes {
        return recordService.get(animationId, user)
    }

    @Authorization
    @PostMapping("")
    fun create(@UserIdentity user: User, @Body form: RecordCreateForm): RecordDetailRes {
        recordService.create(form, user)
        return recordService.get(form.animationId, user)
    }

    @Authorization
    @PatchMapping("/{animationId}")
    fun partialUpdate(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: RecordPartialForm): RecordDetailRes {
        recordService.partialUpdate(animationId, form, user)
        return recordService.get(animationId, user)
    }

    @Authorization
    @DeleteMapping("/{animationId}")
    fun delete(@UserIdentity user: User, @PathVariable animationId: Int): Any? {
        recordService.delete(animationId, user)
        return null
    }

    @Authorization
    @GetMapping("/{animationId}/progress")
    fun progressList(@UserIdentity user: User, @PathVariable animationId: Int): List<ProgressRes> {
        return recordService.getProgressList(animationId, user)
    }

    @Authorization
    @PostMapping("/{animationId}/progress")
    fun progressCreate(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: ProgressForm): ProgressRes {
        return recordService.createProgress(animationId, form, user)
    }
}