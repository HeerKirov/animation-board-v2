package com.heerkirov.animation.authorization


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Authorization(val forge: Boolean = true, val staff: Boolean = false)