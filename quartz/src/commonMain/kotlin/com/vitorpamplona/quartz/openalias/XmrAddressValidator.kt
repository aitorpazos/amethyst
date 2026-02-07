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
 * Validator for Monero (XMR) addresses.
 * 
 * Monero address types:
 * - Standard address: 95 characters, starts with '4'
 * - Subaddress: 95 characters, starts with '8'
 * - Integrated address: 106 characters, starts with '4' (includes payment ID)
 * 
 * All addresses use Base58 encoding.
 */
object XmrAddressValidator {

    // Valid Base58 characters (Bitcoin alphabet, no 0, O, I, l)
    private val BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /**
     * Address type classification.
     */
    enum class AddressType {
        STANDARD,     // 95 chars, starts with '4'
        SUBADDRESS,   // 95 chars, starts with '8'
        INTEGRATED,   // 106 chars, starts with '4' (has embedded payment ID)
        INVALID
    }

    /**
     * Validate an XMR address and return its type.
     * 
     * @param address The address to validate
     * @return The address type, or INVALID if not a valid XMR address
     */
    fun getAddressType(address: String): AddressType {
        val trimmed = address.trim()
        
        // Check length
        return when (trimmed.length) {
            95 -> {
                if (!isValidBase58(trimmed)) {
                    AddressType.INVALID
                } else when {
                    trimmed.startsWith("4") -> AddressType.STANDARD
                    trimmed.startsWith("8") -> AddressType.SUBADDRESS
                    else -> AddressType.INVALID
                }
            }
            106 -> {
                if (!isValidBase58(trimmed)) {
                    AddressType.INVALID
                } else if (trimmed.startsWith("4")) {
                    AddressType.INTEGRATED
                } else {
                    AddressType.INVALID
                }
            }
            else -> AddressType.INVALID
        }
    }

    /**
     * Check if an address is a valid XMR address (any type).
     */
    fun isValid(address: String): Boolean {
        return getAddressType(address) != AddressType.INVALID
    }

    /**
     * Check if an address is a standard XMR address.
     */
    fun isStandardAddress(address: String): Boolean {
        return getAddressType(address) == AddressType.STANDARD
    }

    /**
     * Check if an address is a subaddress.
     */
    fun isSubaddress(address: String): Boolean {
        return getAddressType(address) == AddressType.SUBADDRESS
    }

    /**
     * Check if an address is an integrated address.
     */
    fun isIntegratedAddress(address: String): Boolean {
        return getAddressType(address) == AddressType.INTEGRATED
    }

    /**
     * Check if a string contains only valid Base58 characters.
     */
    private fun isValidBase58(str: String): Boolean {
        return str.all { it in BASE58_CHARS }
    }

    /**
     * Format an XMR address for display (truncated with ellipsis).
     * 
     * @param address The full address
     * @param prefixLength Number of characters to show at the start
     * @param suffixLength Number of characters to show at the end
     * @return Formatted address like "4ABC...XYZ"
     */
    fun formatForDisplay(
        address: String,
        prefixLength: Int = 8,
        suffixLength: Int = 6
    ): String {
        val trimmed = address.trim()
        if (trimmed.length <= prefixLength + suffixLength + 3) {
            return trimmed
        }
        return "${trimmed.take(prefixLength)}...${trimmed.takeLast(suffixLength)}"
    }
}
