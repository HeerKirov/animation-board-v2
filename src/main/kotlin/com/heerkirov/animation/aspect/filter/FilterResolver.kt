package com.heerkirov.animation.aspect.filter

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class FilterResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(Query::class.java)
    }

    override fun resolveArgument(p: MethodParameter, container: ModelAndViewContainer?, request: NativeWebRequest, factory: WebDataBinderFactory?): Any? {
        return parseFilterObject(p.parameterType.kotlin, request.parameterMap)
    }
}