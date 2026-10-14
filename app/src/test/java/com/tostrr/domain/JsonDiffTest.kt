package com.tostrr.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonDiffTest {
    private fun j(text: String): JsonElement = Json.parseToJsonElement(text)
    private fun paths(old: String, new: String) = JsonDiff.diff(j(old), j(new)).map { it.path }

    @Test
    fun identicalTreesHaveNoChange() {
        assertTrue(JsonDiff.diff(j("""{"a":{"b":1}}"""), j("""{"a":{"b":1}}""")).isEmpty())
    }

    @Test
    fun editedScalarIsReportedOnItsOwnPath() {
        val d = JsonDiff.diff(j("""{"a":{"b":1,"c":2}}"""), j("""{"a":{"b":1,"c":3}}""")).single()
        assertEquals("a.c", d.path)
        assertEquals(j("2"), d.old)
        assertEquals(j("3"), d.new)
    }

    @Test
    fun containerEmptiedIsOneChangeNotOnePerField() {
        val d = JsonDiff.diff(
            j("""{"details":{"tasks":{"x":{"y":1,"z":2},"w":3}}}"""),
            j("""{"details":{"tasks":{}}}"""),
        ).single()
        assertEquals("details.tasks", d.path)
        assertEquals(j("""{"x":{"y":1,"z":2},"w":3}"""), d.old)
        assertEquals(j("{}"), d.new)
    }

    @Test
    fun emptyContainerBeingFilledIsOneChange() {
        assertEquals(listOf("a"), paths("""{"a":[]}""", """{"a":[1,2]}"""))
    }

    @Test
    fun addedAndRemovedKeysCarryTheirWholeSubtree() {
        val deltas = JsonDiff.diff(j("""{"gone":{"k":1},"keep":1}"""), j("""{"new":{"k":[1,2]},"keep":1}"""))
        assertEquals(listOf("gone", "new"), deltas.map { it.path })
        assertNull(deltas[0].new)
        assertEquals(j("""{"k":1}"""), deltas[0].old)
        assertNull(deltas[1].old)
        assertEquals(j("""{"k":[1,2]}"""), deltas[1].new)
    }

    @Test
    fun typeChangeIsReportedAtTheContainer() {
        assertEquals(listOf("a"), paths("""{"a":{"b":1}}""", """{"a":"text"}"""))
    }

    @Test
    fun sameSizeArraysAreComparedByIndex() {
        assertEquals(listOf("a.1.n"), paths("""{"a":[{"n":1},{"n":2}]}""", """{"a":[{"n":1},{"n":9}]}"""))
    }

    @Test
    fun differentSizeArraysAreReportedWhole() {
        assertEquals(listOf("a"), paths("""{"a":[1,2]}""", """{"a":[1,2,3]}"""))
    }

    @Test
    fun nullValueIsDistinctFromAbsent() {
        val d = JsonDiff.diff(j("""{"a":{"x":1}}"""), j("""{"a":{"x":1,"y":null}}""")).single()
        assertEquals("a.y", d.path)
        assertNull(d.old)
        assertEquals(j("null"), d.new)
    }
}
