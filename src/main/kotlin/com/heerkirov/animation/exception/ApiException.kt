package com.heerkirov.animation.exception

import com.heerkirov.animation.enums.ErrCode
import java.lang.RuntimeException

open class ApiException(open val code: ErrCode, override val message: String?): RuntimeException(message)