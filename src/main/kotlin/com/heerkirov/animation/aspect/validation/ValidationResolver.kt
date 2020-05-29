package com.heerkirov.animation.aspect.validation

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ApiException
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.parseJsonNode
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.servlet.http.HttpServletRequest

class ValidationResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(p: MethodParameter): Boolean {
        return p.hasParameterAnnotation(Body::class.java)
    }

    @ExperimentalStdlibApi
    override fun resolveArgument(p: MethodParameter, container: ModelAndViewContainer?, request: NativeWebRequest, factory: WebDataBinderFactory?): Any? {
        if(request.getHeader("content-type") != "application/json") throw BadRequestException(ErrCode.INVALID_CONTENT_TYPE, "Content-type must be application/json.")

        val httpServletRequest = request.getNativeRequest(HttpServletRequest::class.java)!!
        val requestBody = InputStreamReader(httpServletRequest.inputStream).use { isr ->
            BufferedReader(isr).use { br ->
                br.readText()
            }
        }
        if(requestBody.isBlank()) throw BadRequestException(ErrCode.EMPTY_REQUEST_BODY, "Request body is empty.")

        val jsonNode = try {
            requestBody.parseJsonNode()
        }catch (e: Throwable) {
            throw BadRequestException(ErrCode.INVALID_REQUEST_BODY, "Cannot parse request body: ${e.message}")
        }

        return mapForm(jsonNode, p.parameterType.kotlin)
    }
}