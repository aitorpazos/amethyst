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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Helper for opening XMR wallet apps to make payments.
 * 
 * Monero URI format: monero:<address>?tx_amount=<amount>&recipient_name=<name>&tx_description=<desc>
 * 
 * Compatible wallets:
 * - Cake Wallet (iOS/Android)
 * - Monerujo (Android)
 * - Feather Wallet (Desktop)
 * - MyMonero (iOS/Android/Desktop)
 */
object XmrPaymentIntent {

    /**
     * Build a Monero payment URI.
     * 
     * @param address XMR address to send to
     * @param amount Optional amount in XMR
     * @param recipientName Optional name of the recipient
     * @param txDescription Optional transaction description/memo
     * @return Monero URI string
     */
    fun buildUri(
        address: String,
        amount: Double? = null,
        recipientName: String? = null,
        txDescription: String? = null
    ): String {
        return buildString {
            append("monero:")
            append(address)
            
            val params = mutableListOf<String>()
            amount?.let { params.add("tx_amount=$it") }
            recipientName?.let { 
                params.add("recipient_name=${URLEncoder.encode(it, "UTF-8")}") 
            }
            txDescription?.let { 
                params.add("tx_description=${URLEncoder.encode(it, "UTF-8")}") 
            }
            
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }

    /**
     * Open an XMR wallet app to make a payment.
     * 
     * @param address XMR address to send to
     * @param amount Optional amount in XMR
     * @param recipientName Optional name of the recipient
     * @param txDescription Optional transaction description/memo
     * @param context Android context
     * @param onSuccess Callback when wallet app is opened
     * @param onError Callback when no wallet app is found
     */
    fun openWallet(
        address: String,
        amount: Double? = null,
        recipientName: String? = null,
        txDescription: String? = null,
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uri = buildUri(address, amount, recipientName, txDescription)
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onSuccess()
        } catch (e: ActivityNotFoundException) {
            onError(
                "No Monero wallet app found. " +
                "Install Cake Wallet or Monerujo to send XMR tips."
            )
        }
    }

    /**
     * Check if any XMR wallet app is installed.
     */
    fun isWalletInstalled(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("monero:"))
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        return activities.isNotEmpty()
    }

    /**
     * Get the Play Store link for Cake Wallet.
     */
    fun getCakeWalletPlayStoreLink(): String {
        return "https://play.google.com/store/apps/details?id=com.cakewallet.cake_wallet"
    }

    /**
     * Get the Play Store link for Monerujo.
     */
    fun getMonerujoPlayStoreLink(): String {
        return "https://play.google.com/store/apps/details?id=com.m2049r.xmrwallet"
    }
}
