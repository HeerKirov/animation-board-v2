package com.heerkirov.animation.aspect

import com.heerkirov.animation.util.logger
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.lang.Exception
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HttpLogInterceptor : HandlerInterceptorAdapter() {
    private val log = logger<HttpLogInterceptor>()

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        log.info("{} {} {}", request.requestURI, request.method, response.status)
    }
}