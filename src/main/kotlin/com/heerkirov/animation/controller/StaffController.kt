package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.enums.CoverType
import com.heerkirov.animation.form.CoverRes
import com.heerkirov.animation.form.StaffForm
import com.heerkirov.animation.form.toRes
import com.heerkirov.animation.model.User
import com.heerkirov.animation.service.CoverService
import com.heerkirov.animation.service.StaffService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/database/staffs")
class StaffController(@Autowired private val staffService: StaffService,
                      @Autowired private val coverService: CoverService) {
    @GetMapping("")
    fun list(): Any {
        //TODO 完成list API的详细功能
        return staffService.list().map { it.toRes() }
    }

    @GetMapping("/{id}")
    fun retrieve(@PathVariable id: Int): Any {
        return staffService.get(id).toRes()
    }

    @Authorization(staff = true)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User, @Body staffForm: StaffForm): Any {
        val id = staffService.create(staffForm, user)
        return staffService.get(id).toRes()
    }

    @Authorization(staff = true)
    @PutMapping("/{id}")
    fun update(@UserIdentity user: User, @PathVariable id: Int, @Body staffForm: StaffForm): Any {
        staffService.update(id, staffForm, user)
        return staffService.get(id).toRes()
    }

    @Authorization(staff = true)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int): Any? {
        staffService.delete(id)
        return null
    }

    @GetMapping("/{id}/cover")
    fun getCover(@PathVariable id: Int): Any {
        return RedirectView(coverService.getCover(CoverType.STAFF, id).toString())
    }

    @PostMapping("/{id}/cover")
    fun uploadCover(@PathVariable id: Int, @RequestParam("file") file: MultipartFile): Any {
        return CoverRes(coverService.uploadCover(CoverType.STAFF, id, file, file.inputStream))
    }
}