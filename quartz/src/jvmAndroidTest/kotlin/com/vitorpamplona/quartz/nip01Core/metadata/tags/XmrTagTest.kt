/**
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.quartz.nip01Core.metadata.tags

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmrTagTest {

    private val validXmrAddress = 
        "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H"

    @Test
    fun testAssemble() {
        val tag = XmrTag.assemble(validXmrAddress)
        assertContentEquals(arrayOf("xmr", validXmrAddress), tag)
    }

    @Test
    fun testParseValid() {
        val tag = arrayOf("xmr", validXmrAddress)
        val result = XmrTag.parse(tag)
        assertEquals(validXmrAddress, result)
    }

    @Test
    fun testParseInvalidTagName() {
        val tag = arrayOf("btc", validXmrAddress)
        val result = XmrTag.parse(tag)
        assertNull(result)
    }

    @Test
    fun testParseEmptyValue() {
        val tag = arrayOf("xmr", "")
        val result = XmrTag.parse(tag)
        assertNull(result)
    }

    @Test
    fun testParseTooShort() {
        val tag = arrayOf("xmr")
        val result = XmrTag.parse(tag)
        assertNull(result)
    }

    @Test
    fun testParseEmptyArray() {
        val tag = emptyArray<String>()
        val result = XmrTag.parse(tag)
        assertNull(result)
    }
}
