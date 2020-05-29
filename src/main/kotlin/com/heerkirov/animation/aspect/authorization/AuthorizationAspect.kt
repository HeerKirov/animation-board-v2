package com.heerkirov.animation.aspect.authorization

import com.heerkirov.animation.exception.AuthenticationException
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.ForbiddenException
import com.heerkirov.animation.model.User
import com.heerkirov.animation.service.AuthService
import com.heerkirov.animation.service.UserService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class AuthorizationAspect(@Autowired private val authService: AuthService,
                          @Autowired private val userService: UserService) {

    @Pointcut("@annotation(Authorization)")
    fun authorization() {}

    @Around("authorization()")
    fun handle(joinPoint: ProceedingJoinPoint): Any? {
        val args = joinPoint.args
        val method = (joinPoint.signature as MethodSignature).method

        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
        val request = attributes!!.request

        val authorization = method.getAnnotation(Authorization::class.java)
        val header = request.getHeader("Authorization") ?: if(authorization.forge) {
            throw AuthenticationException(ErrCode.UNAUTHORIZED, "No authorization token.")
        }else{
            null
        }
        val token = header?.run {
            if(startsWith("Bearer ")) { substring(7) }else{
                throw AuthenticationException(ErrCode.UNAUTHORIZED, "No correct authorization token.")
            }
        }

        val user = if(token == null) null else { authenticate(token) }

        if(authorization.staff && user?.isStaff == false && authorization.forge) {
            throw ForbiddenException(ErrCode.FORBIDDEN, "Forbidden.")
        }

        for (i in method.parameters.indices) {
            if(method.parameters[i].getAnnotation(UserIdentity::class.java) != null) {
                args[i] = when (method.parameters[i].type.kotlin) {
                    String::class -> user?.username
                    User::class -> user
                    Int::class -> user?.id
                    Boolean::class -> user != null
                    else -> throw RuntimeException("Illegal inject type ${method.parameters[i].type}.")
                }
            }
        }

        return joinPoint.proceed(args)
    }

    private fun authenticate(token: String): User? {
        val username = authService.authenticate(token)
        return userService.get(username)
    }
}