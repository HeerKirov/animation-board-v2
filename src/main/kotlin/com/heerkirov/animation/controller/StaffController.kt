package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.enums.CoverType
import com.heerkirov.animation.model.filter.StaffFilter
import com.heerkirov.animation.model.form.*
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.*
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
    fun list(@Query staffFilter: StaffFilter): ListResult<StaffRes> {
        return staffService.list(staffFilter).map { it.toRes() }
    }

    @GetMapping("/{id}")
    fun retrieve(@PathVariable id: Int): StaffRes {
        return staffService.get(id).toRes()
    }

    @Authorization(staff = true)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@UserIdentity user: User, @Body staffForm: StaffForm): StaffRes {
        val id = staffService.create(staffForm, user)
        return staffService.get(id).toRes()
    }

    @Authorization(staff = true)
    @PutMapping("/{id}")
    fun update(@UserIdentity user: User, @PathVariable id: Int, @Body staffForm: StaffForm): StaffRes {
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

    @Authorization(staff = true)
    @PostMapping("/{id}/cover")
    fun uploadCover(@PathVariable id: Int, @RequestParam("file") file: MultipartFile): Any {
        return CoverRes(coverService.uploadCover(CoverType.STAFF, id, file, file.inputStream))
    }
}