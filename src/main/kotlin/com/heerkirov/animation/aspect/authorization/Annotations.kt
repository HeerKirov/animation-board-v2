package com.heerkirov.animation.aspect.authorization

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Authorization(val forge: Boolean = true, val staff: Boolean = false)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class UserIdentity