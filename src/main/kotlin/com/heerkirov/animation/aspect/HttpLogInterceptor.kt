package com.heerkirov.animation.aspect

import com.heerkirov.animation.util.logger
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.lang.Exception
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HttpLogInterceptor : HandlerInterceptorAdapter() {
    private val log = logger<HttpLogInterceptor>()

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        if(request.requestURI.startsWith("/api/database/cover")) {
            //忽略对cover的重定向请求。这些log没什么用，还会污染日志
            return
        }
        log.info("{} {} {}", request.requestURI, request.method, response.status)
    }
}