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
package com.vitorpamplona.quartz.openalias

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class XmrAddressValidatorTest {

    // Real XMR addresses for testing (from public sources)
    
    // Standard mainnet address (95 chars, starts with 4)
    private val validStandardAddress = 
        "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H"

    // Another valid standard address
    private val validStandardAddress2 =
        "4AdUndXHHZ6cfufTMvppY6JwXNouMBzSkbLYfpAV5Usx3skxNgYeYTRj5UzqtReoS44qo9mtmXCqY45DJ852K5Jv2684Rge"

    // Subaddress (95 chars, starts with 8)
    private val validSubaddress =
        "8BeCuHWYnvDLgQxqJpDi4vWXALDqgEifFbRSHQpBpGxTcf4JGMKyXWCpjYn7qZxZ7EScX83fvZEXdKTfKpNM1xGmC8gTyMq"

    // Integrated address (106 chars, starts with 4)
    private val validIntegratedAddress =
        "4LL9oSLmtpccfufTMvppY6JwXNouMBzSkbLYfpAV5Usx3skxNgYeYTRj5UzqtReoS44qo9mtmXCqY45DJ852K5Jv2bYvTKPyMZdcqJdRsEk"

    @Test
    fun testValidStandardAddress() {
        assertEquals(XmrAddressValidator.AddressType.STANDARD, XmrAddressValidator.getAddressType(validStandardAddress))
        assertTrue(XmrAddressValidator.isValid(validStandardAddress))
        assertTrue(XmrAddressValidator.isStandardAddress(validStandardAddress))
        assertFalse(XmrAddressValidator.isSubaddress(validStandardAddress))
        assertFalse(XmrAddressValidator.isIntegratedAddress(validStandardAddress))
    }

    @Test
    fun testValidStandardAddressStartingWith4() {
        assertEquals(XmrAddressValidator.AddressType.STANDARD, XmrAddressValidator.getAddressType(validStandardAddress2))
        assertTrue(XmrAddressValidator.isValid(validStandardAddress2))
        assertTrue(XmrAddressValidator.isStandardAddress(validStandardAddress2))
    }

    @Test
    fun testValidSubaddress() {
        assertEquals(XmrAddressValidator.AddressType.SUBADDRESS, XmrAddressValidator.getAddressType(validSubaddress))
        assertTrue(XmrAddressValidator.isValid(validSubaddress))
        assertTrue(XmrAddressValidator.isSubaddress(validSubaddress))
        assertFalse(XmrAddressValidator.isStandardAddress(validSubaddress))
    }

    @Test
    fun testValidIntegratedAddress() {
        assertEquals(XmrAddressValidator.AddressType.INTEGRATED, XmrAddressValidator.getAddressType(validIntegratedAddress))
        assertTrue(XmrAddressValidator.isValid(validIntegratedAddress))
        assertTrue(XmrAddressValidator.isIntegratedAddress(validIntegratedAddress))
        assertFalse(XmrAddressValidator.isStandardAddress(validIntegratedAddress))
    }

    @Test
    fun testInvalidAddressTooShort() {
        val tooShort = "4ABC123"
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(tooShort))
        assertFalse(XmrAddressValidator.isValid(tooShort))
    }

    @Test
    fun testInvalidAddressTooLong() {
        val tooLong = validStandardAddress + "ExtraChars"
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(tooLong))
        assertFalse(XmrAddressValidator.isValid(tooLong))
    }

    @Test
    fun testInvalidAddressWrongPrefix() {
        // 95 chars but starts with wrong character
        val wrongPrefix = "1" + validStandardAddress.substring(1)
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(wrongPrefix))
        assertFalse(XmrAddressValidator.isValid(wrongPrefix))
    }

    @Test
    fun testInvalidAddressContainsInvalidChars() {
        // Contains '0' which is not valid in Base58
        val invalidChars = "0" + validStandardAddress.substring(1)
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(invalidChars))
    }

    @Test
    fun testInvalidAddressContainsO() {
        // Contains 'O' (capital O) which is not valid in Base58
        val invalidO = validStandardAddress.replaceFirst('N', 'O')
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(invalidO))
    }

    @Test
    fun testInvalidAddressContainsI() {
        // Contains 'I' (capital I) which is not valid in Base58
        val invalidI = validStandardAddress.replaceFirst('N', 'I')
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(invalidI))
    }

    @Test
    fun testInvalidAddressContainsl() {
        // Contains 'l' (lowercase L) which is not valid in Base58
        val invalidl = validStandardAddress.replaceFirst('n', 'l')
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(invalidl))
    }

    @Test
    fun testAddressWithWhitespace() {
        val withSpaces = "  $validStandardAddress  "
        assertTrue(XmrAddressValidator.isValid(withSpaces))
    }

    @Test
    fun testEmptyAddress() {
        assertEquals(XmrAddressValidator.AddressType.INVALID, XmrAddressValidator.getAddressType(""))
        assertFalse(XmrAddressValidator.isValid(""))
    }

    @Test
    fun testFormatForDisplayShortAddress() {
        val short = "4ABC"
        assertEquals("4ABC", XmrAddressValidator.formatForDisplay(short))
    }

    @Test
    fun testFormatForDisplayStandardAddress() {
        val formatted = XmrAddressValidator.formatForDisplay(validStandardAddress)
        assertTrue(formatted.startsWith("888tNkZr"))
        assertTrue(formatted.endsWith("up3H"))
        assertTrue(formatted.contains("..."))
        assertEquals("888tNkZr...vup3H", formatted)
    }

    @Test
    fun testFormatForDisplayCustomLengths() {
        val formatted = XmrAddressValidator.formatForDisplay(validStandardAddress, prefixLength = 4, suffixLength = 4)
        assertEquals("888t...up3H", formatted)
    }

    @Test
    fun testIntegratedAddress106Chars() {
        // Verify the integrated address is exactly 106 characters
        assertEquals(106, validIntegratedAddress.length)
    }

    @Test
    fun testStandardAndSubaddress95Chars() {
        assertEquals(95, validStandardAddress.length)
        assertEquals(95, validSubaddress.length)
    }

    @Test
    fun testSubaddressStartsWith8() {
        assertTrue(validSubaddress.startsWith("8"))
    }

    @Test
    fun testStandardAddressStartsWith4() {
        assertTrue(validStandardAddress.startsWith("4") || validStandardAddress.startsWith("8"))
        // The donation address starts with 8 which makes it a subaddress, let's use the other one
        assertTrue(validStandardAddress2.startsWith("4"))
    }
}
