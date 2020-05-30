package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.enums.CoverType
import com.heerkirov.animation.model.filter.AnimationFilter
import com.heerkirov.animation.model.form.*
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.AnimationService
import com.heerkirov.animation.service.CoverService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/database/animations")
class AnimationController(@Autowired private val animationService: AnimationService,
                          @Autowired private val coverService: CoverService) {
    @Authorization(forge = false)
    @GetMapping("")
    fun list(@UserIdentity user: User?, @Query animationFilter: AnimationFilter): ListResult<AnimationRes> {
        return animationService.list(animationFilter, user).map { it.toRes() }
    }

    @Authorization(forge = false)
    @GetMapping("/{id}")
    fun retrieve(@UserIdentity user: User?, @PathVariable id: Int): AnimationDetailRes {
        return animationService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User, @Body animationForm: AnimationForm): AnimationDetailRes {
        val id = animationService.create(animationForm, user)
        return animationService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @PutMapping("/{id}")
    fun update(@UserIdentity user: User, @PathVariable id: Int, @Body animationForm: AnimationForm): AnimationDetailRes {
        animationService.update(id, animationForm, user)
        return animationService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @PatchMapping("/{id}")
    fun partialUpdate(@UserIdentity user: User, @PathVariable id: Int, @Body animationForm: AnimationPartialForm): AnimationDetailRes {
        animationService.partialUpdate(id, animationForm, user)
        return animationService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int): Any? {
        animationService.delete(id)
        return null
    }

    @GetMapping("/{id}/cover")
    fun getCover(@PathVariable id: Int): Any {
        return RedirectView(coverService.getCover(CoverType.ANIMATION, id).toString())
    }

    @Authorization(staff = true)
    @PostMapping("/{id}/cover")
    fun uploadCover(@PathVariable id: Int, @RequestParam("file") file: MultipartFile): Any {
        return CoverRes(coverService.uploadCover(CoverType.ANIMATION, id, file, file.inputStream))
    }
}