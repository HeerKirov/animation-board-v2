package com.heerkirov.animation.exception

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ApiException

class BadRequestException(override val code: ErrCode, override val message: String?): ApiException(code, message)