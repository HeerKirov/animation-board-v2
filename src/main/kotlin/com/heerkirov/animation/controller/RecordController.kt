package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressCreateForm
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.form.ScatterForm
import com.heerkirov.animation.model.result.NextRes
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordGetterService
import com.heerkirov.animation.service.RecordProgressService
import com.heerkirov.animation.service.RecordScatterService
import com.heerkirov.animation.service.RecordSetterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/personal/records")
class RecordController(@Autowired private val recordGetterService: RecordGetterService,
                       @Autowired private val recordSetterService: RecordSetterService,
                       @Autowired private val recordProgressService: RecordProgressService,
                       @Autowired private val recordScatterService: RecordScatterService) {
    @Authorization
    @PostMapping("")
    fun create(@UserIdentity user: User, @Body form: RecordCreateForm): RecordDetailRes {
        recordSetterService.create(form, user)
        return recordGetterService.get(form.animationId, user)
    }

    @Authorization
    @GetMapping("/{animationId}")
    fun retrieve(@UserIdentity user: User, @PathVariable animationId: Int): RecordDetailRes {
        return recordGetterService.get(animationId, user)
    }

    @Authorization
    @PatchMapping("/{animationId}")
    fun partialUpdate(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: RecordPartialForm): RecordDetailRes {
        recordSetterService.partialUpdate(animationId, form, user)
        return recordGetterService.get(animationId, user)
    }

    @Authorization
    @DeleteMapping("/{animationId}")
    fun delete(@UserIdentity user: User, @PathVariable animationId: Int): Any? {
        recordSetterService.delete(animationId, user)
        return null
    }

    @Authorization
    @PostMapping("/{animationId}/next-episode")
    fun nextEpisode(@UserIdentity user: User, @PathVariable animationId: Int): NextRes {
        return recordProgressService.nextEpisode(animationId, user)
    }

    @Authorization
    @GetMapping("/{animationId}/progress")
    fun progressList(@UserIdentity user: User, @PathVariable animationId: Int): List<ProgressRes> {
        return recordProgressService.getProgressList(animationId, user)
    }

    @Authorization
    @PostMapping("/{animationId}/progress")
    fun progressCreate(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: ProgressCreateForm): ProgressRes {
        return recordProgressService.createProgress(animationId, form, user)
    }

    @Authorization
    @DeleteMapping("/{animationId}/progress/{ordinal}")
    fun progressDelete(@UserIdentity user: User, @PathVariable animationId: Int, @PathVariable ordinal: Int): Any? {
        TODO()
    }

    @Authorization
    @GetMapping("/{animationId}/episode-table")
    fun episodeTable(@UserIdentity user: User, @PathVariable animationId: Int): Any {
        TODO()
    }

    @Authorization
    @PostMapping("/{animationId}/watch-scattered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun watchScattered(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: ScatterForm): Any? {
        recordScatterService.watchScattered(animationId, user, form.episode)
        return null
    }

    @Authorization
    @PostMapping("/{animationId}/group-scattered")
    fun groupScattered(@UserIdentity user: User, @PathVariable animationId: Int) {
        TODO()
    }
}