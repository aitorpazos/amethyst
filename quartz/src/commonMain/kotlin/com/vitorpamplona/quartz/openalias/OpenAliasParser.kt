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

/**
 * Parser for OpenAlias DNS TXT records.
 * 
 * OpenAlias is a standard for cryptocurrency address resolution via DNS.
 * TXT record format: oa1:<crypto> recipient_address=<addr>; recipient_name=<name>; tx_description=<desc>;
 * 
 * See: https://openalias.org/
 */
object OpenAliasParser {

    /**
     * Result of parsing an OpenAlias TXT record.
     */
    data class OpenAliasRecord(
        val cryptoType: String,
        val recipientAddress: String,
        val recipientName: String? = null,
        val txDescription: String? = null,
        val txAmount: String? = null,
        val txPaymentId: String? = null,
        val addressSignature: String? = null,
        val checksum: String? = null
    )

    /**
     * Parse an OpenAlias TXT record string.
     * 
     * @param record The full TXT record string (e.g., "oa1:xmr recipient_address=...;")
     * @return Parsed record, or null if invalid format
     */
    fun parse(record: String): OpenAliasRecord? {
        val trimmed = record.trim()
        
        // Must start with oa1:
        if (!trimmed.startsWith("oa1:")) {
            return null
        }
        
        // Extract crypto type (between "oa1:" and first space)
        val afterPrefix = trimmed.substring(4)
        val spaceIndex = afterPrefix.indexOf(' ')
        if (spaceIndex == -1) {
            return null
        }
        
        val cryptoType = afterPrefix.substring(0, spaceIndex).lowercase()
        val fieldsSection = afterPrefix.substring(spaceIndex + 1)
        
        // Parse fields
        val recipientAddress = extractField(fieldsSection, "recipient_address") ?: return null
        
        return OpenAliasRecord(
            cryptoType = cryptoType,
            recipientAddress = recipientAddress,
            recipientName = extractField(fieldsSection, "recipient_name"),
            txDescription = extractField(fieldsSection, "tx_description"),
            txAmount = extractField(fieldsSection, "tx_amount"),
            txPaymentId = extractField(fieldsSection, "tx_payment_id"),
            addressSignature = extractField(fieldsSection, "address_signature"),
            checksum = extractField(fieldsSection, "checksum")
        )
    }

    /**
     * Parse an OpenAlias TXT record for a specific cryptocurrency.
     * 
     * @param record The full TXT record string
     * @param cryptoType The expected cryptocurrency type (e.g., "xmr", "btc")
     * @return Parsed record if crypto type matches, null otherwise
     */
    fun parseForCrypto(record: String, cryptoType: String): OpenAliasRecord? {
        val parsed = parse(record) ?: return null
        return if (parsed.cryptoType == cryptoType.lowercase()) parsed else null
    }

    /**
     * Extract a field value from the fields section of an OpenAlias record.
     * Fields are in format: field_name=value;
     */
    private fun extractField(fieldsSection: String, fieldName: String): String? {
        val regex = Regex("""$fieldName=([^;]+)""")
        val match = regex.find(fieldsSection) ?: return null
        return match.groupValues[1].trim()
    }

    /**
     * Convert an OpenAlias address (user@domain.com) to FQDN for DNS lookup.
     * 
     * @param openAliasAddress Address in format user@domain.com
     * @return FQDN (user.domain.com), or null if invalid format
     */
    fun toFqdn(openAliasAddress: String): String? {
        val parts = openAliasAddress.split("@")
        if (parts.size != 2) {
            return null
        }
        val user = parts[0].trim()
        val domain = parts[1].trim()
        if (user.isEmpty() || domain.isEmpty()) {
            return null
        }
        return "$user.$domain"
    }

    /**
     * Check if a string looks like an OpenAlias address (user@domain.com format).
     */
    fun isOpenAliasFormat(address: String): Boolean {
        val parts = address.split("@")
        return parts.size == 2 && 
               parts[0].isNotEmpty() && 
               parts[1].contains(".") &&
               !parts[1].startsWith(".") &&
               !parts[1].endsWith(".")
    }
}
