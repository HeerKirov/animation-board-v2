package com.heerkirov.animation.service

import com.heerkirov.animation.model.filter.StaffFilter
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.form.StaffForm
import com.heerkirov.animation.model.data.Staff
import com.heerkirov.animation.model.data.User

interface StaffService {
    fun list(filter: StaffFilter): ListResult<Staff>

    fun get(id: Int): Staff

    fun create(staffForm: StaffForm, creator: User): Int

    fun update(id: Int, staffForm: StaffForm, updater: User)

    fun delete(id: Int)
}