package com.heerkirov.animation.form

import com.heerkirov.animation.exception.ApiException
import com.heerkirov.animation.enums.ErrCode

data class ErrResult(val code: ErrCode, val message: String) {
    constructor(e: ApiException): this(e.code, e.message ?: "Internal server error")
}