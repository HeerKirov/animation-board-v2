package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.form.TagForm
import com.heerkirov.animation.form.toDetailRes
import com.heerkirov.animation.form.toRes
import com.heerkirov.animation.model.User
import com.heerkirov.animation.service.TagService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/database/tags")
class TagController(@Autowired private val tagService: TagService) {
    @GetMapping("")
    fun list(): Any {
        //TODO 完成list API的详细功能
        return tagService.list().map { it.toRes() }
    }

    @GetMapping("/{id}")
    fun retrieve(@PathVariable id: Int): Any {
        return tagService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User, @Body tagForm: TagForm): Any {
        val id = tagService.create(tagForm, user)
        return tagService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @PutMapping("/{id}")
    fun update(@UserIdentity user: User, @PathVariable id: Int, @Body tagForm: TagForm): Any {
        tagService.update(id, tagForm, user)
        return tagService.get(id).toDetailRes()
    }

    @Authorization(staff = true)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int): Any? {
        tagService.delete(id)
        return null
    }
}