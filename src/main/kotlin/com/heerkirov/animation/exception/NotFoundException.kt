package com.heerkirov.animation.exception

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ApiException

class NotFoundException(override val message: String?): ApiException(ErrCode.NOT_FOUND, message)