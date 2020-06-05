package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.CommentActivityFilter
import com.heerkirov.animation.model.filter.CommentFindFilter
import com.heerkirov.animation.model.filter.RankFilter
import com.heerkirov.animation.model.form.CommentCreateForm
import com.heerkirov.animation.model.form.CommentUpdateForm
import com.heerkirov.animation.model.result.CommentFindRes
import com.heerkirov.animation.model.result.CommentRankRes
import com.heerkirov.animation.model.result.CommentRes
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.service.CommentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/personal/comments")
class CommentController(@Autowired private val commentService: CommentService) {
    @Authorization
    @GetMapping("/activity")
    fun activity(@UserIdentity user: User, @Query filter: CommentActivityFilter): ListResult<CommentRes> {
        return commentService.activity(filter, user)
    }

    @Authorization
    @GetMapping("/rank")
    fun rank(@UserIdentity user: User, @Query filter: RankFilter): ListResult<CommentRankRes> {
        return commentService.rank(filter, user)
    }

    @Authorization
    @GetMapping("/find")
    fun find(@UserIdentity user: User, @Query filter: CommentFindFilter): ListResult<CommentFindRes> {
        return commentService.find(filter, user)
    }

    @Authorization
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User, @Body form: CommentCreateForm): CommentRes {
        commentService.create(form, user)
        return commentService.get(form.animationId, user)
    }

    @Authorization
    @GetMapping("/{animationId}")
    fun retrieve(@UserIdentity user: User, @PathVariable animationId: Int): CommentRes {
        return commentService.get(animationId, user)
    }

    @Authorization
    @PatchMapping("/{animationId}")
    fun partialUpdate(@UserIdentity user: User, @PathVariable animationId: Int, @Body form: CommentUpdateForm): CommentRes {
        commentService.partialUpdate(animationId, form, user)
        return commentService.get(animationId, user)
    }

    @Authorization
    @DeleteMapping("/{animationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@UserIdentity user: User, @PathVariable animationId: Int): Any? {
        commentService.delete(animationId, user)
        return null
    }
}