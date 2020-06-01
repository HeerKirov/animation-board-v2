package com.heerkirov.animation.util

import com.heerkirov.animation.enums.RelationType
import com.heerkirov.animation.util.relation.RelationGraph
import kotlin.test.Test
import kotlin.test.assertEquals

class RelationGraphTest {
    @Test
    fun testFullRelation() {
        val graph = RelationGraph<String, RelationType>(arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")) {
            addRelation("A", RelationType.NEXT, "B")
            addRelation("B", RelationType.RUMOR, "H")
            addRelation("C", RelationType.PREV, "B")
            addRelation("C", RelationType.NEXT, "D")
            addRelation("D", RelationType.FANWAI, "F")
            addRelation("E", RelationType.MAIN_ARTICLE, "C")
            addRelation("G", RelationType.NEXT, "H")
            addRelation("I", RelationType.TRUE_PASS, "D")
            addRelation("J", RelationType.SERIES, "H")
            addRelation("K", RelationType.NEXT, "L")
        }
        assertEquals(mapOf(
                RelationType.NEXT to listOf("B", "C", "D", "E", "F"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["A"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("A"),
                RelationType.NEXT to listOf("C", "D", "E", "F"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["B"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("A", "B"),
                RelationType.NEXT to listOf("D", "F"),
                RelationType.FANWAI to listOf("E"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["C"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("A", "B", "C", "E"),
                RelationType.FANWAI to listOf("F"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["D"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("A", "B"),
                RelationType.NEXT to listOf("D", "F"),
                RelationType.MAIN_ARTICLE to listOf("C"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["E"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("A", "B", "C", "E"),
                RelationType.MAIN_ARTICLE to listOf("D"),
                RelationType.RUMOR to listOf("G", "H", "I"),
                RelationType.SERIES to listOf("J")
        ), graph["F"])
        assertEquals(mapOf(
                RelationType.NEXT to listOf("H"),
                RelationType.TRUE_PASS to listOf("A", "B", "C", "D", "E", "F"),
                RelationType.SERIES to listOf("I", "J")
        ), graph["G"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("G"),
                RelationType.TRUE_PASS to listOf("A", "B", "C", "D", "E", "F"),
                RelationType.SERIES to listOf("I", "J")
        ), graph["H"])
        assertEquals(mapOf(
                RelationType.TRUE_PASS to listOf("A", "B", "C", "D", "E", "F"),
                RelationType.SERIES to listOf("G", "H", "J")
        ), graph["I"])
        assertEquals(mapOf(
                RelationType.SERIES to listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
        ), graph["J"])
        assertEquals(mapOf(
                RelationType.NEXT to listOf("L")
        ), graph["K"])
        assertEquals(mapOf(
                RelationType.PREV to listOf("K")
        ), graph["L"])
    }
}