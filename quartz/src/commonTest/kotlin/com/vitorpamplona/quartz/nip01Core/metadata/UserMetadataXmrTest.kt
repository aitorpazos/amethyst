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
package com.vitorpamplona.quartz.nip01Core.metadata

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserMetadataXmrTest {

    private val validXmrAddress = 
        "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H"
    
    private val validOpenAlias = "donate@getmonero.org"

    @Test
    fun testXmrFieldsExist() {
        val metadata = UserMetadata()
        assertNull(metadata.xmr)
        assertNull(metadata.xmrOpenAlias)
    }

    @Test
    fun testSetXmrAddress() {
        val metadata = UserMetadata()
        metadata.xmr = validXmrAddress
        assertEquals(validXmrAddress, metadata.xmr)
        assertEquals(validXmrAddress, metadata.xmrAddress())
    }

    @Test
    fun testSetXmrOpenAlias() {
        val metadata = UserMetadata()
        metadata.xmrOpenAlias = validOpenAlias
        assertEquals(validOpenAlias, metadata.xmrOpenAlias)
        assertEquals(validOpenAlias, metadata.xmrAddress())
    }

    @Test
    fun testXmrAddressPrefersDirectAddress() {
        val metadata = UserMetadata()
        metadata.xmr = validXmrAddress
        metadata.xmrOpenAlias = validOpenAlias
        
        // xmrAddress() should prefer direct address over OpenAlias
        assertEquals(validXmrAddress, metadata.xmrAddress())
    }

    @Test
    fun testXmrAddressFallsBackToOpenAlias() {
        val metadata = UserMetadata()
        metadata.xmrOpenAlias = validOpenAlias
        
        // When xmr is null, should return xmrOpenAlias
        assertEquals(validOpenAlias, metadata.xmrAddress())
    }

    @Test
    fun testCleanBlankNamesTrimsXmr() {
        val metadata = UserMetadata()
        metadata.xmr = "  $validXmrAddress  "
        metadata.xmrOpenAlias = "  $validOpenAlias  "
        
        metadata.cleanBlankNames()
        
        assertEquals(validXmrAddress, metadata.xmr)
        assertEquals(validOpenAlias, metadata.xmrOpenAlias)
    }

    @Test
    fun testCleanBlankNamesNullifiesBlankXmr() {
        val metadata = UserMetadata()
        metadata.xmr = "   "
        metadata.xmrOpenAlias = "   "
        
        metadata.cleanBlankNames()
        
        assertNull(metadata.xmr)
        assertNull(metadata.xmrOpenAlias)
    }

    @Test
    fun testAnyNameStartsWithIncludesXmr() {
        val metadata = UserMetadata()
        metadata.xmr = validXmrAddress
        
        assertTrue(metadata.anyNameStartsWith("888t"))
    }

    @Test
    fun testAnyNameStartsWithIncludesOpenAlias() {
        val metadata = UserMetadata()
        metadata.xmrOpenAlias = validOpenAlias
        
        assertTrue(metadata.anyNameStartsWith("donate@"))
        assertTrue(metadata.anyNameStartsWith("getmonero"))
    }

    @Test
    fun testJsonSerializationWithXmr() {
        val json = Json { ignoreUnknownKeys = true }
        
        val jsonString = """
            {
                "name": "Test User",
                "xmr": "$validXmrAddress",
                "xmr_openalias": "$validOpenAlias"
            }
        """.trimIndent()
        
        val metadata = json.decodeFromString<UserMetadata>(jsonString)
        
        assertEquals("Test User", metadata.name)
        assertEquals(validXmrAddress, metadata.xmr)
        assertEquals(validOpenAlias, metadata.xmrOpenAlias)
    }

    @Test
    fun testJsonSerializationWithoutXmr() {
        val json = Json { ignoreUnknownKeys = true }
        
        val jsonString = """
            {
                "name": "Test User",
                "lud16": "test@example.com"
            }
        """.trimIndent()
        
        val metadata = json.decodeFromString<UserMetadata>(jsonString)
        
        assertEquals("Test User", metadata.name)
        assertEquals("test@example.com", metadata.lud16)
        assertNull(metadata.xmr)
        assertNull(metadata.xmrOpenAlias)
    }

    @Test
    fun testXmrAddressWithBothNull() {
        val metadata = UserMetadata()
        assertNull(metadata.xmrAddress())
    }
}
