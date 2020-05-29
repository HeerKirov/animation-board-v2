package com.heerkirov.animation.aspect

import com.heerkirov.animation.util.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class HttpAspect {
    private val log = logger<HttpAspect>()

    @Around("execution(* com.heerkirov.animation.controller..*(..))") @Throws(Throwable::class)
    fun handle(joinPoint: ProceedingJoinPoint): Any? {
        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
        val request = attributes!!.request
        log.info("{} {}", request.method, request.requestURI)

        return joinPoint.proceed()
    }
}