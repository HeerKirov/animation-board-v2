package com.heerkirov.animation.exception

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ApiException

class InternalException(override val message: String?): ApiException(ErrCode.INTERNAL_SERVER_ERROR, message)