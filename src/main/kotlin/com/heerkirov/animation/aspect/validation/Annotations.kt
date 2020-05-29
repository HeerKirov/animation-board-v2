package com.heerkirov.animation.aspect.validation

/**
 * Spring Web注解：将Controller参数标注为使用form parser模块进行解析。
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body

/**
 * Form注解：为构造器参数显示标注json field命名
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Field(val value: String = "")

/**
 * Valid注解：此String字段必须非空
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NotBlank

/**
 * Valid注解：此String字段不可超出最大长度
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class MaxLength(val value: Int)

/**
 * Valid注解：此Number字段值必须位于指定范围内
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Range(val min: Int, val max: Int)

/**
 * Valid注解：此Number字段值必须不小于指定值
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Min(val value: Int)
