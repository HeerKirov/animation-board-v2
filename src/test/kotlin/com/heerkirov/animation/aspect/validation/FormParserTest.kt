package com.heerkirov.animation.aspect.validation

import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.parseJsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*

class FormParserTest {
    @Test fun testPrimitive() {
        //基础类型测试
        assertEquals(TestUser("heer", 12, "a", true),
                """{"name": "heer", "age": 12, "address": "a", "fav": true}""".toForm()
        )
        assertEquals(TestType(1, 1L, 1.1F, 1.1, "s",
                LocalDate.of(2020, 2, 2),
                LocalDateTime.of(2020, 2, 2, 12, 34, 56)),
                """{"int": 1, "long": 1, "float": 1.1, "double": 1.1, "string": "s", "date": "2020-02-02", "datetime": "2020-02-02T12:34:56Z"}""".toForm()
        )
        //不一致的基础类型
        assertFailsWith<BadRequestException> {
            """{"string": 100}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"string": false}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"string": 1.1}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"int": 1.1}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"int": "1"}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"int": true}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"long": 1.1}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"long": "1"}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"long": true}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"float": "1.11"}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"double": "1.11"}""".toForm<TestType>()
        }
        //错误的日期格式
        assertFailsWith<BadRequestException> {
            """{"date": "2020"}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"datetime": "2020-02-02 12:34:56"}""".toForm<TestType>()
        }
    }

    @Test fun testEnum() {
        assertEquals(TestType(enum = Enum.A),
                """{"enum": "A"}""".toForm()
        )
        assertEquals(TestType(enum = Enum.B),
                """{"enum": "B"}""".toForm()
        )
        assertEquals(TestType(enum = Enum.C),
                """{"enum": "C"}""".toForm()
        )
        assertEquals(TestType(enum = Enum.A),
                """{"enum": "a"}""".toForm()
        )
        assertFailsWith<BadRequestException> {
            """{"enum": "D"}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"enum": true}""".toForm<TestType>()
        }
        assertFailsWith<BadRequestException> {
            """{"enum": 0}""".toForm<TestType>()
        }
    }

    @Test fun testNullable() {
        //可空参数
        assertEquals(TestUser("heer", null, null, true),
                """{"name": "heer", "age": null, "address": null, "fav": true}""".toForm()
        )
        //不可空参数为空
        assertFailsWith<BadRequestException> {
            """{"name": "heer", "age": 12, "address": "a", "fav": null}""".toForm<TestUser>()
        }
    }

    @Test fun testOptional() {
        //可选参数
        assertEquals(TestUser("heer", 12, null, false),
                """{"name": "heer", "age": 12}""".toForm()
        )
        //缺失非可选参数
        assertFailsWith<BadRequestException> {
            """{"name": "heer"}""".toForm<TestUser>()
        }
        //无参数
        assertFailsWith<BadRequestException> {
            """{}""".toForm<TestUser>()
        }
    }

    @Test fun testNested() {
        //嵌套参数
        assertEquals(TestUser("heer", 12, best = TestUser("02", 2)),
                """{"name": "heer", "age": 12, "best": {"name": "02", "age": 2}}""".toForm()
        )
        assertEquals(TestUser("heer", 12, friends = arrayListOf(TestUser("02", 2))),
                """{"name": "heer", "age": 12, "friends": [{"name": "02", "age": 2}]}""".toForm()
        )
        assertEquals(TestUser("heer", 12, friendsMap = mapOf("a" to TestUser("02", 2))),
                """{"name": "heer", "age": 12, "friends_map": {"a": {"name": "02", "age": 2}}}""".toForm()
        )
        assertEquals(TestUser("heer", 12, array = arrayListOf(1, 2, 3)),
                """{"name": "heer", "age": 12, "array": [1, 2, 3]}""".toForm()
        )
        assertEquals(TestUser("heer", 12, map = mapOf("a" to null, "b" to 1, "c" to "c", "d" to false)),
                """{"name": "heer", "age": 12, "map": {"a": null, "b": 1, "c": "c", "d": false}}""".toForm()
        )
        assertEquals(TestUser("heer", 12, map = mapOf("a" to arrayListOf(1, 2), "b" to arrayListOf())),
                """{"name": "heer", "age": 12, "map": {"a": [1, 2], "b": []}}""".toForm()
        )
        assertFailsWith<BadRequestException> {
            """{"name": "heer", "age": 12, "array": [1, 2, 3, null]}""".toForm<TestUser>()
        }
        assertFailsWith<BadRequestException> {
            """{"name": "heer", "age": 12, "friends_map": {"a": {"name": "02", "age": 2}, "b": null}}""".toForm<TestUser>()
        }
    }

    @Test fun testValid() {
        assertEquals(TestValid(s1 = "1", s2 = "1234567890", i1 = 0, i2 = 0),
                """{"s1": "1", "s2": "1234567890", "i1": 0, "i2": 0}""".toForm()
        )
        assertEquals(TestValid(i1 = -5),
                """{"i1": -5}""".toForm()
        )
        assertEquals(TestValid(i1 = 5),
                """{"i1": 5}""".toForm()
        )
        assertFailsWith<BadRequestException> {
            """{"s1": " "}""".toForm<TestValid>()
        }
        assertFailsWith<BadRequestException> {
            """{"s1": ""}""".toForm<TestValid>()
        }
        assertFailsWith<BadRequestException> {
            """{"s2": "12345678900"}""".toForm<TestValid>()
        }
        assertFailsWith<BadRequestException> {
            """{"i1": "-6"}""".toForm<TestValid>()
        }
        assertFailsWith<BadRequestException> {
            """{"i1": "6"}""".toForm<TestValid>()
        }
        assertFailsWith<BadRequestException> {
            """{"i2": "-1"}""".toForm<TestValid>()
        }
    }

    private inline fun <reified T: Any> String.toForm(): T {
        return mapForm(this.parseJsonNode(), T::class)
    }

    data class TestUser(@Field("name") val name: String,
                        @Field("age") val age: Int?,
                        @Field("address") val address: String? = null,
                        @Field("fav") val fav: Boolean = false,
                        @Field("best") val best: TestUser? = null,
                        @Field("friends") val friends: List<TestUser> = arrayListOf(),
                        @Field("friends_map") val friendsMap: Map<String, TestUser> = mapOf(),
                        @Field("array") val array: List<Int> = arrayListOf(),
                        @Field("map") val map: Map<String, Any?> = mapOf())

    data class TestType(@Field("int") val int: Int? = null,
                        @Field("long") val long: Long? = null,
                        @Field("float") val float: Float? = null,
                        @Field("double") val double: Double? = null,
                        @Field("string") val string: String? = null,
                        @Field("date") val date: LocalDate? = null,
                        @Field("datetime") val datetime: LocalDateTime? = null,
                        @Field("enum") val enum: Enum? = null)

    data class TestValid(@NotBlank val s1: String? = null,
                         @MaxLength(10) val s2: String? = null,
                         @Range(min = -5, max = 5) val i1: Int? = null,
                         @Min(0) val i2: Int? = null)

    enum class Enum {
        A, B, C
    }
}