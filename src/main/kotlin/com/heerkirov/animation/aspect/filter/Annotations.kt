package com.heerkirov.animation.aspect.filter

/**
 * Spring Web注解：将Controller参数标注为使用filter parser进行解析。
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Query

/**
 * Filter Form构造参数注解：在此参数注入limit
 * limit参数必定为Int
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Limit(val value: String = "limit")

/**
 * Filter Form构造参数注解：在此参数注入offset
 * offset参数必定为Int
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Offset(val value: String = "offset")

/**
 * Filter Form构造参数注解：在此参数注入search
 * search参数必定为String
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Search(val value: String = "search")

/**
 * Filter Form构造参数注解：在此参数注入order
 * order参数必定为List<(Pair<String, Int>|Pair<(Int), String>)>
 * 可以指定选项列表以限制可选值。指定选项列表后可指定是否忽略大小写
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Order(val value: String = "order",
                       val options: Array<String> = [],
                       val default: String = "",
                       val ignoreCase: Boolean = true,
                       val delimiter: String = ",")

/**
 * Filter Form构造参数注解：在此参数注入自定义的filter值
 * filter参数使用form parser解析器进行根解析，因此可以为其指定非常复杂的类型
 * 可以指定选项列表以限制可选值。指定选项列表后可指定是否忽略大小写
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Filter(val value: String,
                        val options: Array<String> = [],
                        val ignoreCase: Boolean = true,
                        val delimiter: String = ",")