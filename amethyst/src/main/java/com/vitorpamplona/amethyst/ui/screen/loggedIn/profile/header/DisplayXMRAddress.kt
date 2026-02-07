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
package com.vitorpamplona.amethyst.ui.screen.loggedIn.profile.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.Amethyst
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.openalias.OpenAliasResolver
import com.vitorpamplona.amethyst.service.openalias.XmrPaymentIntent
import com.vitorpamplona.amethyst.ui.components.ClickableTextPrimary
import com.vitorpamplona.amethyst.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.ui.note.ErrorMessageDialog
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.stringRes
import com.vitorpamplona.amethyst.ui.theme.MoneroOrange
import com.vitorpamplona.amethyst.ui.theme.Size16Modifier
import com.vitorpamplona.quartz.openalias.XmrAddressValidator
import kotlinx.coroutines.launch

@Composable
fun DisplayXMRAddress(
    xmrAddress: String?,
    xmrOpenAlias: String?,
    user: User,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    
    var tipExpanded by remember { mutableStateOf(false) }
    var resolvedAddress by remember { mutableStateOf<String?>(null) }
    var recipientName by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var tipAmount by remember { mutableStateOf("") }

    // Resolve OpenAlias if needed
    LaunchedEffect(xmrOpenAlias) {
        if (!xmrOpenAlias.isNullOrEmpty() && xmrAddress.isNullOrEmpty()) {
            isResolving = true
            scope.launch {
                val resolver = OpenAliasResolver()
                val result = resolver.resolveXmr(xmrOpenAlias) { url ->
                    Amethyst.instance.okHttpClients.defaultHttpClientWithoutProxy.value
                }
                resolvedAddress = result?.recipientAddress
                recipientName = result?.recipientName
                isResolving = false
            }
        }
    }

    val displayAddress = xmrAddress ?: resolvedAddress
    val displayLabel = when {
        !xmrOpenAlias.isNullOrEmpty() -> xmrOpenAlias
        !xmrAddress.isNullOrEmpty() -> XmrAddressValidator.formatForDisplay(xmrAddress)
        else -> null
    }

    // Error dialog
    if (showErrorDialog != null) {
        ErrorMessageDialog(
            title = stringRes(id = R.string.error_dialog_zap_error),
            textContent = showErrorDialog ?: "",
            onClickStartMessage = null,
            onDismiss = { showErrorDialog = null },
        )
    }

    // Only show if we have an address to display
    if (displayLabel != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            MoneroIcon(modifier = Size16Modifier, tint = MoneroOrange)

            ClickableTextPrimary(
                text = if (isResolving) "$displayLabel (resolving...)" else displayLabel,
                onClick = { tipExpanded = !tipExpanded },
                modifier = Modifier
                    .padding(top = 1.dp, bottom = 1.dp, start = 5.dp)
                    .weight(1f),
            )

            // Copy button
            if (displayAddress != null) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(displayAddress))
                        accountViewModel.toastManager.toast(
                            "Copied",
                            "XMR address copied to clipboard"
                        )
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy XMR address",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Expanded tip card
        if (tipExpanded && displayAddress != null) {
            XmrTipCard(
                address = displayAddress,
                recipientName = recipientName ?: user.info.bestName(),
                tipAmount = tipAmount,
                onTipAmountChange = { tipAmount = it },
                onSendTip = {
                    val amount = tipAmount.toDoubleOrNull()
                    XmrPaymentIntent.openWallet(
                        address = displayAddress,
                        amount = amount,
                        recipientName = recipientName ?: user.info.bestName(),
                        context = context,
                        onSuccess = { tipExpanded = false },
                        onError = { error -> showErrorDialog = error }
                    )
                },
                onInstallWallet = {
                    uriHandler.openUri(XmrPaymentIntent.getCakeWalletPlayStoreLink())
                },
                isWalletInstalled = XmrPaymentIntent.isWalletInstalled(context),
                onDismiss = { tipExpanded = false }
            )
        }
    }
}

@Composable
private fun XmrTipCard(
    address: String,
    recipientName: String?,
    tipAmount: String,
    onTipAmountChange: (String) -> Unit,
    onSendTip: () -> Unit,
    onInstallWallet: () -> Unit,
    isWalletInstalled: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Send XMR Tip",
                style = MaterialTheme.typography.titleMedium,
                color = MoneroOrange
            )

            if (recipientName != null) {
                Text(
                    text = "To: $recipientName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = XmrAddressValidator.formatForDisplay(address, 12, 8),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = tipAmount,
                onValueChange = onTipAmountChange,
                label = { Text("Amount (XMR)") },
                placeholder = { Text("0.01") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                if (isWalletInstalled) {
                    Button(
                        onClick = onSendTip,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneroOrange
                        )
                    ) {
                        Text("Open Wallet")
                    }
                } else {
                    Button(
                        onClick = onInstallWallet,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneroOrange
                        )
                    ) {
                        Text("Get Wallet")
                    }
                }
            }
        }
    }
}

@Composable
fun MoneroIcon(
    modifier: Modifier = Modifier,
    tint: Color = MoneroOrange
) {
    Icon(
        painter = androidx.compose.ui.res.painterResource(id = com.vitorpamplona.amethyst.R.drawable.monero),
        contentDescription = "Monero",
        tint = Color.Unspecified, // Use original colors from vector
        modifier = modifier
    )
}
