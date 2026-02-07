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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class OpenAliasParserTest {

    // Real OpenAlias record from donate.getmonero.org
    private val realMoneroRecord = 
        "oa1:xmr recipient_address=888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H; recipient_name=Monero Development; tx_description=Donation to Monero Core Team;"

    private val realBtcRecord =
        "oa1:btc recipient_address=1KTexdemPdxSBcG55heUuTjDRYqbC5ZL8H; recipient_name=Monero Development; tx_description=Donation to Monero Core Team;"

    @Test
    fun testParseValidXmrRecord() {
        val result = OpenAliasParser.parse(realMoneroRecord)
        
        assertNotNull(result)
        assertEquals("xmr", result.cryptoType)
        assertEquals("888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H", result.recipientAddress)
        assertEquals("Monero Development", result.recipientName)
        assertEquals("Donation to Monero Core Team", result.txDescription)
    }

    @Test
    fun testParseValidBtcRecord() {
        val result = OpenAliasParser.parse(realBtcRecord)
        
        assertNotNull(result)
        assertEquals("btc", result.cryptoType)
        assertEquals("1KTexdemPdxSBcG55heUuTjDRYqbC5ZL8H", result.recipientAddress)
        assertEquals("Monero Development", result.recipientName)
    }

    @Test
    fun testParseMinimalRecord() {
        val minimal = "oa1:xmr recipient_address=4ABC123;"
        val result = OpenAliasParser.parse(minimal)
        
        assertNotNull(result)
        assertEquals("xmr", result.cryptoType)
        assertEquals("4ABC123", result.recipientAddress)
        assertNull(result.recipientName)
        assertNull(result.txDescription)
    }

    @Test
    fun testParseWithAllFields() {
        val full = "oa1:xmr recipient_address=4ABC; recipient_name=Test; tx_description=Desc; tx_amount=1.5; tx_payment_id=abc123;"
        val result = OpenAliasParser.parse(full)
        
        assertNotNull(result)
        assertEquals("4ABC", result.recipientAddress)
        assertEquals("Test", result.recipientName)
        assertEquals("Desc", result.txDescription)
        assertEquals("1.5", result.txAmount)
        assertEquals("abc123", result.txPaymentId)
    }

    @Test
    fun testParseInvalidPrefix() {
        val invalid = "oa2:xmr recipient_address=4ABC;"
        val result = OpenAliasParser.parse(invalid)
        assertNull(result)
    }

    @Test
    fun testParseMissingRecipientAddress() {
        val missing = "oa1:xmr recipient_name=Test;"
        val result = OpenAliasParser.parse(missing)
        assertNull(result)
    }

    @Test
    fun testParseEmptyString() {
        val result = OpenAliasParser.parse("")
        assertNull(result)
    }

    @Test
    fun testParseForCryptoMatchingType() {
        val result = OpenAliasParser.parseForCrypto(realMoneroRecord, "xmr")
        assertNotNull(result)
        assertEquals("xmr", result.cryptoType)
    }

    @Test
    fun testParseForCryptoNonMatchingType() {
        val result = OpenAliasParser.parseForCrypto(realMoneroRecord, "btc")
        assertNull(result)
    }

    @Test
    fun testParseForCryptoCaseInsensitive() {
        val result = OpenAliasParser.parseForCrypto(realMoneroRecord, "XMR")
        assertNotNull(result)
    }

    @Test
    fun testToFqdnValid() {
        val fqdn = OpenAliasParser.toFqdn("donate@getmonero.org")
        assertEquals("donate.getmonero.org", fqdn)
    }

    @Test
    fun testToFqdnWithSpaces() {
        val fqdn = OpenAliasParser.toFqdn(" user @ domain.com ")
        assertEquals("user.domain.com", fqdn)
    }

    @Test
    fun testToFqdnInvalidNoAt() {
        val fqdn = OpenAliasParser.toFqdn("notanemail")
        assertNull(fqdn)
    }

    @Test
    fun testToFqdnInvalidMultipleAt() {
        val fqdn = OpenAliasParser.toFqdn("user@domain@extra.com")
        assertNull(fqdn)
    }

    @Test
    fun testToFqdnInvalidEmptyUser() {
        val fqdn = OpenAliasParser.toFqdn("@domain.com")
        assertNull(fqdn)
    }

    @Test
    fun testToFqdnInvalidEmptyDomain() {
        val fqdn = OpenAliasParser.toFqdn("user@")
        assertNull(fqdn)
    }

    @Test
    fun testIsOpenAliasFormatValid() {
        assertTrue(OpenAliasParser.isOpenAliasFormat("donate@getmonero.org"))
        assertTrue(OpenAliasParser.isOpenAliasFormat("user@sub.domain.com"))
        assertTrue(OpenAliasParser.isOpenAliasFormat("a@b.c"))
    }

    @Test
    fun testIsOpenAliasFormatInvalid() {
        assertFalse(OpenAliasParser.isOpenAliasFormat("notanemail"))
        assertFalse(OpenAliasParser.isOpenAliasFormat("user@"))
        assertFalse(OpenAliasParser.isOpenAliasFormat("@domain.com"))
        assertFalse(OpenAliasParser.isOpenAliasFormat("user@domain"))  // no TLD
        assertFalse(OpenAliasParser.isOpenAliasFormat("user@.domain.com"))
        assertFalse(OpenAliasParser.isOpenAliasFormat("user@domain."))
    }

    @Test
    fun testParseRecordWithTrailingWhitespace() {
        val record = "  oa1:xmr recipient_address=4ABC123;  "
        val result = OpenAliasParser.parse(record)
        
        assertNotNull(result)
        assertEquals("4ABC123", result.recipientAddress)
    }

    @Test
    fun testParseCryptoTypeCaseNormalization() {
        val uppercase = "oa1:XMR recipient_address=4ABC;"
        val result = OpenAliasParser.parse(uppercase)
        
        assertNotNull(result)
        assertEquals("xmr", result.cryptoType)
    }
}
