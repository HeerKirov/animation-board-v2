package com.heerkirov.animation.aspect

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ApiException
import com.heerkirov.animation.exception.AuthenticationException
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.form.ErrResult
import com.heerkirov.animation.util.logger
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class ApiExceptionHandler {
    private val log: Logger = logger<ApiExceptionHandler>()

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun authenticationFailed(e: AuthenticationException): ErrResult = ErrResult(e)

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: BadRequestException): ErrResult = ErrResult(e)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(e: NotFoundException): ErrResult = ErrResult(e)

    @ExceptionHandler(ApiException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(e: ApiException): ErrResult = ErrResult(e)

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(e: Throwable): ErrResult {
        log.error("error occurred. ", e)
        return ErrResult(ErrCode.INTERNAL_SERVER_ERROR, e.message ?: "Internal server error")
    }
}