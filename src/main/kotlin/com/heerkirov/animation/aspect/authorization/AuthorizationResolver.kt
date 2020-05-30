package com.heerkirov.animation.aspect.authorization

import com.heerkirov.animation.model.data.User
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class AuthorizationResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(p: MethodParameter): Boolean {
        return p.hasParameterAnnotation(UserIdentity::class.java) &&
                (p.parameterType.kotlin == User::class ||
                        p.parameterType.kotlin == Int::class ||
                        p.parameterType.kotlin == String::class)
    }

    override fun resolveArgument(p: MethodParameter, container: ModelAndViewContainer?, request: NativeWebRequest, factory: WebDataBinderFactory?): Any? {
        return when(p.parameterType.kotlin) {
            Int::class -> 0
            String::class -> null
            User::class -> null
            else -> throw RuntimeException("Illegal inject type ${p.parameterType}.")
        }
    }
}