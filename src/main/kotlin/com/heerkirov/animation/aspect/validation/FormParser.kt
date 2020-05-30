package com.heerkirov.animation.aspect.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.map
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toDate
import com.heerkirov.animation.util.toDateTime
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlin.ClassCastException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

/**
 * 执行将jsonNode转换为任意Object定义的过程。
 * @throws NullPointerException 类型指定为非空，然而获得了空类型
 * @throws ClassCastException 类型转换上遇到了错误
 */
private fun <T : Any> mapAny(jsonNode: JsonNode?, kType: KType): Any? {
    @Suppress("UNCHECKED_CAST")
    val kClass = kType.classifier as KClass<*>

    @Suppress("UNCHECKED_CAST")
    return when {
        jsonNode == null || jsonNode.isNull -> {
            if(!kType.isMarkedNullable) throw NullPointerException()
            null
        }
        kClass == List::class || kClass == Set::class -> {
            if(jsonNode.nodeType != JsonNodeType.ARRAY) throw ClassCastException("Excepted type is ${JsonNodeType.ARRAY} but actual type is ${jsonNode.nodeType}.")
            val subType = kType.arguments.first().type!!
            try {
                jsonNode.map { mapAny<Any>(it, subType) }
            }catch (e: NullPointerException) {
                throw ClassCastException("Element of array cannot be null.")
            }
        }
        kClass == Map::class -> {
            if(jsonNode.nodeType != JsonNodeType.OBJECT) throw ClassCastException("Excepted type is ${JsonNodeType.OBJECT} but actual type is ${jsonNode.nodeType}.")
            val keyType = kType.arguments[0].type!!
            val valueType = kType.arguments[1].type!!
            try {
                jsonNode.fields().map { entry -> Pair(mapString(entry.key, keyType), mapAny<Any>(entry.value, valueType)) }.toMap()
            }catch (e: NullPointerException) {
                throw ClassCastException("Value of object cannot be null.")
            }
        }
        kClass == String::class -> {
            if(jsonNode.nodeType != JsonNodeType.STRING) throw ClassCastException("Excepted type is ${JsonNodeType.STRING} but actual type is ${jsonNode.nodeType}.")
            jsonNode.asText() as T
        }
        kClass == Boolean::class -> {
            if(jsonNode.nodeType != JsonNodeType.BOOLEAN) throw ClassCastException("Excepted type is ${JsonNodeType.BOOLEAN} but actual type is ${jsonNode.nodeType}.")
            jsonNode.asBoolean() as T
        }
        kClass == Int::class -> {
            if(jsonNode.nodeType != JsonNodeType.NUMBER) throw ClassCastException("Excepted type is ${JsonNodeType.NUMBER} but actual type is ${jsonNode.nodeType}.")
            if(!jsonNode.isInt && !jsonNode.isLong) throw ClassCastException("Excepted number type of Int.")
            jsonNode.asInt() as T
        }
        kClass == Long::class -> {
            if(jsonNode.nodeType != JsonNodeType.NUMBER) throw ClassCastException("Excepted type is ${JsonNodeType.NUMBER} but actual type is ${jsonNode.nodeType}.")
            if(!jsonNode.isInt && !jsonNode.isLong) throw ClassCastException("Excepted number type of Long.")
            jsonNode.asLong() as T
        }
        kClass == Double::class -> {
            if(jsonNode.nodeType != JsonNodeType.NUMBER) throw ClassCastException("Excepted type is ${JsonNodeType.NUMBER} but actual type is ${jsonNode.nodeType}.")
            jsonNode.asDouble() as T
        }
        kClass == Float::class -> {
            if(jsonNode.nodeType != JsonNodeType.NUMBER) throw ClassCastException("Excepted type is ${JsonNodeType.NUMBER} but actual type is ${jsonNode.nodeType}.")
            jsonNode.asDouble().toFloat() as T
        }
        kClass == LocalDateTime::class -> {
            if(jsonNode.nodeType != JsonNodeType.STRING) throw ClassCastException("Excepted type is ${JsonNodeType.STRING} but actual type is ${jsonNode.nodeType}.")
            try {
                jsonNode.asText().toDateTime() as T
            }catch (e: DateTimeParseException) {
                throw ClassCastException(e.message)
            }
        }
        kClass == LocalDate::class -> {
            if(jsonNode.nodeType != JsonNodeType.STRING) throw ClassCastException("Excepted type is ${JsonNodeType.STRING} but actual type is ${jsonNode.nodeType}.")
            try {
                jsonNode.asText().toDate() as T
            }catch (e: DateTimeParseException) {
                throw ClassCastException(e.message)
            }
        }
        kClass == Any::class -> mapAnyWithoutType(jsonNode)
        kClass.isData -> {
            //提取非空参数，进行递归解析
            if(jsonNode.nodeType != JsonNodeType.OBJECT) throw ClassCastException("Excepted type is ${JsonNodeType.OBJECT} but actual type is ${jsonNode.nodeType}.")
            mapForm(jsonNode, kClass)
        }
        kClass.isSubclassOf(Enum::class) -> {
            if(jsonNode.nodeType != JsonNodeType.STRING) throw ClassCastException("Excepted type is ${JsonNodeType.STRING} but actual type is ${jsonNode.nodeType}.")
            val value = jsonNode.asText()
            val valueOf = kClass.java.getDeclaredMethod("valueOf", String::class.java)
            try {
                valueOf(null, value.toUpperCase())
            }catch (e: Exception) {
                throw ClassCastException("Cannot convert '$value' to enum type ${kClass.simpleName}.")
            }
        }
        else -> throw IllegalArgumentException("Cannot analyse argument of type '$kClass'.")
    }
}

/**
 * 执行将string类型按照kType定义转换为任意object的过程。
 */
fun mapString(string: String, kType: KType): Any? {
    @Suppress("UNCHECKED_CAST")
    val kClass = kType.classifier as KClass<*>
    @Suppress("UNCHECKED_CAST")
    return when (kClass) {
        String::class -> string
        Int::class -> string.toIntOrNull() ?: throw ClassCastException("Expected number type of Int.")
        Long::class -> string.toLongOrNull() ?: throw ClassCastException("Expected number type of Long.")
        Float::class -> string.toFloatOrNull() ?: throw ClassCastException("Expected number type of Float.")
        Double::class -> string.toDoubleOrNull() ?: throw ClassCastException("Expected number type of Double.")
        Boolean::class -> string.toBoolean()
        LocalDateTime::class -> {
            try {
                string.toDateTime()
            }catch (e: DateTimeParseException) {
                throw ClassCastException(e.message)
            }
        }
        LocalDate::class -> {
            try {
                string.toDate()
            }catch (e: DateTimeParseException) {
                throw ClassCastException(e.message)
            }
        }
        else -> throw IllegalArgumentException("Cannot analyse argument of type '$kClass'.")
    }
}

/**
 * 执行将任意jsonNode在未知类型情况下自动转换为object的过程。
 */
private fun mapAnyWithoutType(jsonNode: JsonNode): Any {
    return when(jsonNode.nodeType) {
        JsonNodeType.NUMBER -> if(jsonNode.isInt || jsonNode.isLong) {
            jsonNode.asInt()
        }else{
            jsonNode.asDouble()
        }
        JsonNodeType.STRING -> jsonNode.asText()
        JsonNodeType.BOOLEAN -> jsonNode.asBoolean()
        JsonNodeType.ARRAY -> jsonNode.parseJSONObject()
        JsonNodeType.OBJECT -> jsonNode.parseJSONObject()
        else -> throw ClassCastException("Cannot parse type ${jsonNode.nodeType}.")
    }
}

/**
 * 执行将jsonNode转换为Form定义的Object的过程。
 * @throws BadRequestException 遇到了转换错误
 */
fun <T : Any> mapForm(jsonNode: JsonNode, formClass: KClass<T>): T {
    val constructor = formClass.primaryConstructor!!

    val args = constructor.parameters.map { parameter ->
        val fieldAnnotation = parameter.annotations.firstOrNull { it is Field } as Field?

        val optional = parameter.isOptional
        val name = if(fieldAnnotation != null && fieldAnnotation.value.isNotEmpty()) {
            fieldAnnotation.value
        }else{
            parameter.name
        }!!

        when {
            //form中包含对此field的定义，将其提取出来
            jsonNode.has(name) -> {
                val node = jsonNode.get(name)
                val value = try {
                    mapAny<Any>(node, parameter.type)
                }catch (e: ClassCastException) {
                    throw BadRequestException(ErrCode.TYPE_ERROR, "Param '$name' cast error: ${e.message}")
                }catch (e: NullPointerException) {
                    throw BadRequestException(ErrCode.PARAM_ERROR, "Param '$name' cannot be null.")
                }

                if(value != null) {
                    try {
                        analyseValidation(parameter.annotations, name, value)
                    }catch (e: Exception) {
                        throw BadRequestException(ErrCode.PARAM_ERROR, e.message)
                    }
                }

                Pair(parameter, value)
            }
            //form中不包含field的定义，但是参数定义表示此field可选
            optional -> null
            //不包含定义且必选，那么将抛出参数请求的异常
            else -> throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param '$name' is required.")
        }
    }.filterNotNull().toMap()

    return constructor.callBy(args)
}

/**
 * 解析附带的validation检验注解，并执行检验。
 */
private fun analyseValidation(annotations: List<Annotation>, name: String, value: Any) {
    if(value is String) {
        (annotations.firstOrNull { it is NotBlank } as NotBlank?)?.let {
            if(value.isBlank()) throw Exception("Param '$name' cannot be blank.")
        }
        (annotations.firstOrNull { it is MaxLength } as MaxLength?)?.let {
            if(value.length > it.value) throw Exception("Param '$name' cannot longer than ${value.length}.")
        }
    }else if(value is Number) {
        val i = value.toInt()
        (annotations.firstOrNull { it is Range } as Range?)?.let {
            if(i > it.max || i < it.min) throw Exception("Param '$name' must be in range [${it.min}, ${it.max}].")
        }
        (annotations.firstOrNull { it is Min } as Min?)?.let {
            if(i < it.value) throw Exception("Param '$name' must be greater than ${it.value}.")
        }
    }
}