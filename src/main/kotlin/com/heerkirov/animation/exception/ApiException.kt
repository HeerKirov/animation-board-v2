package com.heerkirov.animation.exception

import com.heerkirov.animation.enums.ErrCode

open class ApiException(open val code: ErrCode, override val message: String?): RuntimeException(message)