package com.heerkirov.animation.aspect.filter

import com.heerkirov.animation.aspect.validation.mapString
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.reduce
import java.lang.NumberFormatException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/* filter parser后续可优化方向
    - search参数支持自动切割和查询参数生成
    - 所有参数支持kotlin原生默认值(isOptional)
 */

fun parseFilterObject(kClass: KClass<*>, parameterMap: Map<String, Array<String>>): Any {
    val constructor = kClass.primaryConstructor!!

    val args = constructor.parameters.map { parameter ->
        val value = when(val annotation = parameter.annotations.firstOrNull { it is Search || it is Order || it is Limit || it is Offset || it is Filter }) {
            is Search -> parseSearchParameter(takeParameterValue(parameterMap, annotation.value))
            is Limit -> parseLimitParameter(annotation, takeParameterValue(parameterMap, annotation.value))
            is Offset -> parseOffsetParameter(annotation, takeParameterValue(parameterMap, annotation.value))
            is Order -> parseOrderParameter(annotation, parameter.type, takeParameterValues(parameterMap, annotation.value, annotation.delimiter))
            is Filter -> parseFilterParameter(annotation, parameter.type, takeParameterValue(parameterMap, annotation.value))
            else -> throw UnsupportedOperationException()
        }

        Pair(parameter, value)
    }.toMap()

    return constructor.callBy(args)
}

private fun takeParameterValue(parameterMap: Map<String, Array<String>>, key: String): String? {
    return parameterMap[key]?.firstOrNull()?.ifBlank { null }
}

private fun takeParameterValues(parameterMap: Map<String, Array<String>>, key: String, delimiter: String): Array<String> {
    val arr = parameterMap[key] ?: return emptyArray()
    return arr.map { it -> it.split(delimiter).map { it.trim() }.filter { it.isNotBlank() } }.reduce().toTypedArray()
}

private fun parseSearchParameter(parameterValue: String?): String? {
    return parameterValue
}

private fun parseLimitParameter(limit: Limit, parameterValue: String?): Int? {
    return if(parameterValue != null) {
        val value = try { parameterValue.toInt() }catch (e: NumberFormatException) {
            throw BadRequestException(ErrCode.TYPE_ERROR, "Param '${limit.value}' must be integer.")
        }
        if(value < 0) {
            throw BadRequestException(ErrCode.PARAM_ERROR, "Param '${limit.value}' must be greater than or equal 0.")
        }
        value
    }else{
        null
    }
}

private fun parseOffsetParameter(offset: Offset, parameterValue: String?): Int? {
    return if(parameterValue != null) {
        val value = try { parameterValue.toInt() }catch (e: NumberFormatException) {
            throw BadRequestException(ErrCode.TYPE_ERROR, "Param '${offset.value}' must be integer.")
        }
        if(value < 0) {
            throw BadRequestException(ErrCode.PARAM_ERROR, "Param '${offset.value}' must be greater than or equal 0.")
        }
        value
    }else{
        null
    }
}

private fun parseOrderParameter(order: Order, kType: KType, parameterValues: Array<String>): Any? {
    //首先进行繁多的类型检查
    //最外层必须为List
    if(kType.classifier != List::class) {
        throw RuntimeException("Illegal inject type '${kType.classifier}'. It must be List<?>.")
    }
    //下一层必须为Pair
    val pairType = kType.arguments.first().type
    if(pairType?.classifier != Pair::class) {
        throw RuntimeException("Illegal inject type '${pairType?.classifier}'. It must be List<Pair<?, ?>>.")
    }
    //Pair内可以为<String, Int>或<Int, String>
    val keyClassifier = pairType.arguments[0].type?.classifier
    val valueClassifier = pairType.arguments[1].type?.classifier
    val si = if(keyClassifier == String::class && valueClassifier == Int::class) true
            else if(keyClassifier == Int::class && valueClassifier == String::class) false
            else throw RuntimeException("Illegal inject type <$keyClassifier, $valueClassifier>. It must be List<Pair<Int, String>> or List<Pair<String, Int>>.")

    //提取出选项列表。由于有不区分大小写还要还原拼写的需要，制作成map
    val options = if(order.options.isNotEmpty()) {
        if(order.ignoreCase) {
            order.options.map { Pair(it.toLowerCase(), it) }.toMap()
        }else{
            order.options.map { Pair(it, it) }.toMap()
        }
    }else{
        null
    }

    val parameterPairs = when {
        //用户填写的字段不为空值
        parameterValues.isNotEmpty() -> parameterValues.asSequence().map(::mapOrderPair).let {
            if(options != null) {
                //在存在可选项的情况下，需要对每一项进行比对，匹配的项取其原拼写放回，一旦不匹配就抛出异常
                it.map { (direction, field) ->
                    val match = options[if(order.ignoreCase) field.toLowerCase() else field]
                            ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param '${order.value}' must be in [${options.values.joinToString(", ")}].")
                    Pair(direction, match)
                }
            }else{
                it
            }
        }.toList()
        //用户没有填写，但是存在默认值
        order.default.isNotBlank() -> arrayListOf(mapOrderPair(order.default))
        //否则就直接返回null
        else -> return null
    }

    //最后根据Pair的泛型参数顺序做调整
    return if(si) {
        parameterPairs.map { (direction, field) -> Pair(field, direction) }
    }else{
        parameterPairs
    }
}

private fun parseFilterParameter(filter: Filter, kType: KType, parameterValue: String?): Any? {
    val value = if(parameterValue == null) {
        if(filter.default.isBlank()) {
            return null
        }else{
            filter.default
        }
    }else if(filter.options.isNotEmpty()) {
        filter.options.map { option ->
            if((filter.ignoreCase && option.toLowerCase() == parameterValue.toLowerCase()) || (!filter.ignoreCase && option == parameterValue)) {
                option
            }else{
                null
            }
        }.firstOrNull { it != null } ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param '${filter.value}' must be in [${filter.options.joinToString(", ")}].")
    }else{
        parameterValue
    }

    try {
        return mapString(value, kType)
    }catch (e: ClassCastException) {
        throw BadRequestException(ErrCode.PARAM_ERROR, "Param '${filter.value}' cast error: ${e.message}")
    }
}

private fun mapOrderPair(it: String): Pair<Int, String> {
    return when {
        it.startsWith('+') -> Pair(1, it.substring(1))
        it.startsWith('-') -> Pair(-1, it.substring(1))
        else -> Pair(1, it)
    }
}