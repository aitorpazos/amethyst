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
package com.vitorpamplona.amethyst.service.openalias

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vitorpamplona.quartz.openalias.OpenAliasParser
import com.vitorpamplona.quartz.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import kotlin.coroutines.cancellation.CancellationException

/**
 * OpenAlias resolver for cryptocurrency addresses.
 * Resolves user@domain.com format addresses to actual crypto addresses via DNS TXT records.
 * 
 * Uses DNS-over-HTTPS (Cloudflare) to fetch DNS records, avoiding Android DNS API limitations.
 * 
 * See: https://openalias.org/
 */
class OpenAliasResolver {
    
    companion object {
        private const val TAG = "OpenAliasResolver"
        private const val DOH_CLOUDFLARE = "https://cloudflare-dns.com/dns-query"
    }

    /**
     * Resolve an OpenAlias address to an XMR address.
     * 
     * @param openAliasAddress Address in format: user@domain.com
     * @param okHttpClient OkHttpClient factory for making DNS-over-HTTPS requests
     * @return Resolved OpenAlias record, or null if resolution failed
     */
    suspend fun resolveXmr(
        openAliasAddress: String,
        okHttpClient: (String) -> OkHttpClient
    ): OpenAliasParser.OpenAliasRecord? {
        return resolve(openAliasAddress, "xmr", okHttpClient)
    }

    /**
     * Resolve an OpenAlias address for any cryptocurrency type.
     * 
     * @param openAliasAddress Address in format: user@domain.com
     * @param cryptoType Cryptocurrency prefix (e.g., "xmr", "btc")
     * @param okHttpClient OkHttpClient factory for making DNS-over-HTTPS requests
     * @return Resolved OpenAlias record, or null if resolution failed
     */
    suspend fun resolve(
        openAliasAddress: String,
        cryptoType: String,
        okHttpClient: (String) -> OkHttpClient
    ): OpenAliasParser.OpenAliasRecord? {
        val fqdn = OpenAliasParser.toFqdn(openAliasAddress) ?: return null
        
        val txtRecords = fetchDnsTxtRecords(fqdn, okHttpClient)
        
        return txtRecords
            .mapNotNull { OpenAliasParser.parseForCrypto(it, cryptoType) }
            .firstOrNull()
    }

    /**
     * Fetch DNS TXT records using DNS-over-HTTPS (Cloudflare).
     * This avoids issues with Android's limited DNS API access.
     */
    private suspend fun fetchDnsTxtRecords(
        fqdn: String,
        okHttpClient: (String) -> OkHttpClient
    ): List<String> {
        val url = "$DOH_CLOUDFLARE?name=$fqdn&type=TXT"
        val client = okHttpClient(url)
        
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
            
            client.newCall(request).executeAsync().use { response ->
                withContext(Dispatchers.IO) {
                    if (response.isSuccessful) {
                        parseDnsJsonResponse(response.body.string())
                    } else {
                        Log.d(TAG, "DNS lookup failed: ${response.code} for $fqdn")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error fetching DNS TXT for $fqdn", e)
            emptyList()
        }
    }

    /**
     * Parse Cloudflare DNS-over-HTTPS JSON response.
     * Response format: { "Answer": [{ "data": "\"txt record content\"" }] }
     */
    private fun parseDnsJsonResponse(json: String): List<String> {
        return try {
            val mapper = jacksonObjectMapper()
            val tree = mapper.readTree(json)
            val answers = tree.get("Answer") ?: return emptyList()
            
            answers.mapNotNull { answer ->
                val data = answer.get("data")?.asText()
                // DNS TXT records are quoted in the JSON response
                data?.trim('"')
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing DNS JSON response", e)
            emptyList()
        }
    }
}
