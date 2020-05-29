package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.model.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/database/animations")
class AnimationController {
    @Authorization(forge = false)
    @GetMapping("")
    fun list(@UserIdentity user: User?): Any {
        TODO()
    }

    @Authorization(forge = false)
    @GetMapping("/{id}")
    fun retrieve(@UserIdentity user: User?, @PathVariable id: Int): Any {
        TODO()
    }

    @Authorization(staff = true)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User): Any {
        TODO()
    }

    @Authorization(staff = true)
    @PutMapping("/{id}")
    fun update(@UserIdentity user: User, @PathVariable id: Int): Any {
        TODO()
    }

    @Authorization(staff = true)
    @PatchMapping("/{id}")
    fun partialUpdate(@UserIdentity user: User, @PathVariable id: Int): Any {
        TODO()
    }

    @Authorization(staff = true)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@UserIdentity user: User, @PathVariable id: Int): Any {
        TODO()
    }
}