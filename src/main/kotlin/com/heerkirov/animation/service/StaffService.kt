package com.heerkirov.animation.service

import com.heerkirov.animation.form.StaffForm
import com.heerkirov.animation.model.Staff
import com.heerkirov.animation.model.User

interface StaffService {
    fun list(): List<Staff>

    fun get(id: Int): Staff

    fun create(staffForm: StaffForm, creator: User): Int

    fun update(id: Int, staffForm: StaffForm, updater: User)

    fun delete(id: Int)
}