package com.heerkirov.animation.aspect.filter

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.reduce
import org.springframework.core.MethodParameter
import java.lang.NumberFormatException
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

fun parseFilterObject(p: MethodParameter, kClass: KClass<*>, parameterMap: Map<String, Array<String>>): Any {
    val constructor = kClass.primaryConstructor!!

    val args = constructor.parameters.map { parameter ->
        val value = when(val annotation = parameter.annotations.firstOrNull { it is Search || it is Order || it is Limit || it is Offset || it is Filter }) {
            is Search -> parseSearchParameter(annotation, parameter.type, takeParameterValue(parameterMap, parameter, annotation.value))
            is Limit -> parseLimitParameter(annotation, parameter.type, takeParameterValue(parameterMap, parameter, annotation.value))
            is Offset -> parseOffsetParameter(annotation, parameter.type, takeParameterValue(parameterMap, parameter, annotation.value))
            is Order -> parseOrderParameter(annotation, parameter.type, takeParameterValues(parameterMap, parameter, annotation.value, annotation.delimiter))
            is Filter -> null
            else -> throw UnsupportedOperationException()
        }

        Pair(parameter, value)
    }.toMap()

    return constructor.callBy(args)
}

private fun takeParameterValue(parameterMap: Map<String, Array<String>>, parameter: KParameter, key: String): String? {
    return parameterMap[key]?.firstOrNull()?.ifBlank { null }
}

private fun takeParameterValues(parameterMap: Map<String, Array<String>>, parameter: KParameter, key: String, delimiter: String): Array<String> {
    val arr = parameterMap[key] ?: return emptyArray()
    return arr.map { it -> it.split(delimiter).filter { it.isNotBlank() } }.reduce().toTypedArray()
}

private fun parseSearchParameter(search: Search, kType: KType, parameterValue: String?): String? {
    return parameterValue
}

private fun parseLimitParameter(limit: Limit, kType: KType, parameterValue: String?): Int? {
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

private fun parseOffsetParameter(offset: Offset, kType: KType, parameterValue: String?): Int? {
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
    if(kType.classifier != List::class) {
        throw RuntimeException("Illegal inject type '${kType.classifier}'. It must be List<?>.")
    }
    val pairType = kType.arguments.first().type
    if(pairType != Pair::class) {
        throw RuntimeException("Illegal inject type '${kType.classifier}'. It must be List<Pair<?, ?>>.")
    }



    val parameterPairs = parameterValues.map {
        when {
            it.startsWith('+') -> Pair(1, it.substring(1))
            it.startsWith('-') -> Pair(-1, it.substring(1))
            else -> Pair(1, it)
        }
    }

    val keyType = pairType.arguments[0].type
    val valueType = pairType.arguments[1].type
    val si = if(keyType == String::class && valueType == Int::class) true
            else if(keyType == Int::class && valueType == String::class) false
            else throw RuntimeException("Illegal inject type '${kType.classifier}'. It must be List<Pair<Int, String>> or List<Pair<String, Int>>.")

    val values = if(si) {
        parameterPairs.map { Pair(it.second, it.first) }
    }else{
        parameterPairs
    }

    //TODO 写完……

    if(values.isEmpty() && order.default.isNotBlank()) {
        return if(si) {
            when {
                order.default.startsWith('+') -> Pair(1, order.default.substring(1))
                order.default.startsWith('-') -> Pair(-1, order.default.substring(1))
                else -> Pair(1, order.default)
            }
        }else{
            when {
                order.default.startsWith('+') -> Pair(order.default.substring(1), 1)
                order.default.startsWith('-') -> Pair(order.default.substring(1), -1)
                else -> Pair(order.default, -1)
            }
        }
    }

    return if(order.options.isNotEmpty()) {

    }else{
        values
    }
}