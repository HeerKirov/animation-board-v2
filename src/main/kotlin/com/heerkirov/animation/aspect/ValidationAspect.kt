package com.heerkirov.animation.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

@Aspect
@Component
class ValidationAspect {
    @ExperimentalStdlibApi
    @Around("execution(* com.heerkirov.animation.controller..*(..))")
    fun handle(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method

        val httpMethod = when {
            method.getAnnotation(PostMapping::class.java) != null -> "POST"
            method.getAnnotation(PutMapping::class.java) != null -> "PUT"
            method.getAnnotation(PatchMapping::class.java) != null -> "PATCH"
            else -> return joinPoint.proceed()
        }

        val args = joinPoint.args
        for (i in method.parameters.indices) {
            val parameter = method.parameters[i]
            if(parameter.getAnnotation(RequestBody::class.java) != null) {
                val functions = parameter.type.kotlin.functions
                val validate = when(httpMethod) {
                    "POST" -> {
                        functions.firstOrNull { it.hasAnnotation<ValidateInCreating>() }?:
                        functions.firstOrNull { it.hasAnnotation<Validate>() }
                    }
                    "PUT" -> {
                        functions.firstOrNull { it.hasAnnotation<ValidateInUpdating>() }?:
                        functions.firstOrNull { it.hasAnnotation<Validate>() }
                    }
                    "PATCH" -> {
                        functions.firstOrNull { it.hasAnnotation<ValidateInPartialUpdating>() }?:
                        functions.firstOrNull { it.hasAnnotation<ValidateInUpdating>() }?:
                        functions.firstOrNull { it.hasAnnotation<Validate>() }
                    }
                    else -> null
                }
                try {
                    validate?.call(args[i])
                }catch (e: InvocationTargetException) {
                    throw e.targetException
                }
                break
            }
        }

        return joinPoint.proceed()
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Validate

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ValidateInCreating

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ValidateInUpdating

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ValidateInPartialUpdating