package com.heerkirov.animation.form

import com.heerkirov.animation.aspect.Validate
import com.heerkirov.animation.aspect.validation.Form
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.Tag
import com.heerkirov.animation.util.toDateTimeString

data class TagRes(val id: Int, val name: String)

data class TagDetailRes(val id: Int, val name: String, val introduction: String?, val createTime: String, val updateTime: String)

@Form
data class TagForm(val name: String,
                   val introduction: String) {
    @Validate
    fun validate() {
        if(name.isBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Name cannot be empty.")
    }
}

fun Tag.toRes() = TagRes(id, name)

fun Tag.toDetailRes() = TagDetailRes(id, name, introduction, createTime.toDateTimeString(), updateTime.toDateTimeString())