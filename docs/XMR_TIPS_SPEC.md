# Amethyst XMR Fork — Technical Specification

## Overview

Fork of [Amethyst](https://github.com/vitorpamplona/amethyst) (Nostr client for Android) to add Monero (XMR) tipping support with OpenAlias address resolution.

## Objectives

1. **XMR Address Field** — Add XMR address to Nostr user profiles
2. **OpenAlias Resolution** — Resolve `user@domain.com` to XMR addresses via DNS TXT records
3. **Tip Flow** — Deep-link to XMR wallets (Cake Wallet, Monerujo) for payments
4. **Display** — Show XMR address in profile header alongside Lightning address

---

## Architecture

### 1. Data Model Changes

**File:** `quartz/src/commonMain/kotlin/com/vitorpamplona/quartz/nip01Core/metadata/UserMetadata.kt`

```kotlin
@Stable
@Serializable
class UserMetadata {
    // ... existing fields ...
    var lud06: String? = null
    var lud16: String? = null
    
    // NEW: Monero fields
    var xmr: String? = null           // Direct XMR address (4... or 8...)
    var xmrOpenAlias: String? = null  // OpenAlias format: user@domain.com
    
    // Helper method
    fun xmrAddress(): String? = xmr ?: xmrOpenAlias
}
```

**New Tag Files:**
- `quartz/src/commonMain/kotlin/com/vitorpamplona/quartz/nip01Core/metadata/tags/XmrTag.kt`
- `quartz/src/commonMain/kotlin/com/vitorpamplona/quartz/nip01Core/metadata/tags/XmrOpenAliasTag.kt`

### 2. OpenAlias Resolution

**New File:** `amethyst/src/main/java/com/vitorpamplona/amethyst/service/openalias/OpenAliasResolver.kt`

```kotlin
class OpenAliasResolver {
    /**
     * Resolve OpenAlias address to XMR address via DNS TXT record
     * Format: oa1:xmr recipient_address=<addr>; recipient_name=<name>; tx_description=<desc>;
     */
    suspend fun resolve(openAliasAddress: String): OpenAliasResult? {
        val parts = openAliasAddress.split("@")
        if (parts.size != 2) return null
        
        val fqdn = "${parts[0]}.${parts[1]}"
        val txtRecords = fetchDnsTxt(fqdn)
        
        return txtRecords
            .filter { it.startsWith("oa1:xmr") }
            .firstNotNullOfOrNull { parseOpenAliasRecord(it) }
    }
    
    private suspend fun fetchDnsTxt(fqdn: String): List<String> {
        // Use Android's DnsResolver or OkHttp DNS-over-HTTPS
        // Cloudflare: https://cloudflare-dns.com/dns-query?name=<fqdn>&type=TXT
    }
    
    private fun parseOpenAliasRecord(record: String): OpenAliasResult? {
        // Parse: oa1:xmr recipient_address=...; recipient_name=...; tx_description=...;
        val regex = Regex("""recipient_address=([^;]+)""")
        val match = regex.find(record) ?: return null
        return OpenAliasResult(
            address = match.groupValues[1].trim(),
            name = extractField(record, "recipient_name"),
            description = extractField(record, "tx_description")
        )
    }
}

data class OpenAliasResult(
    val address: String,
    val name: String? = null,
    val description: String? = null
)
```

### 3. XMR Address Display

**New File:** `amethyst/src/main/java/com/vitorpamplona/amethyst/ui/screen/loggedIn/profile/header/DisplayXMRAddress.kt`

```kotlin
@Composable
fun DisplayXMRAddress(
    xmrAddress: String?,
    xmrOpenAlias: String?,
    user: User,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    var tipExpanded by remember { mutableStateOf(false) }
    var resolvedAddress by remember { mutableStateOf<String?>(null) }
    
    // Resolve OpenAlias if needed
    LaunchedEffect(xmrOpenAlias) {
        if (!xmrOpenAlias.isNullOrEmpty() && xmrAddress.isNullOrEmpty()) {
            resolvedAddress = OpenAliasResolver().resolve(xmrOpenAlias)?.address
        }
    }
    
    val displayAddress = xmrAddress ?: resolvedAddress
    val displayLabel = xmrOpenAlias ?: xmrAddress?.take(12) + "..."
    
    if (displayAddress != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneroIcon(modifier = Size16Modifier, tint = MoneroOrange)
            
            ClickableTextPrimary(
                text = displayLabel,
                onClick = { tipExpanded = !tipExpanded },
                modifier = Modifier.padding(top = 1.dp, bottom = 1.dp, start = 5.dp)
            )
        }
        
        if (tipExpanded) {
            XmrTipCard(
                address = displayAddress,
                onTip = { amount ->
                    // Deep link to wallet
                    openXmrWallet(displayAddress, amount)
                }
            )
        }
    }
}
```

### 4. XMR Payment Intent

**New File:** `amethyst/src/main/java/com/vitorpamplona/amethyst/ui/note/XmrPaymentIntent.kt`

```kotlin
/**
 * Open XMR wallet app for payment
 * URI format: monero:<address>?tx_amount=<amount>&recipient_name=<name>
 */
fun openXmrWallet(
    address: String,
    amount: Double? = null,
    recipientName: String? = null,
    context: Context,
    onError: (String) -> Unit
) {
    val uri = buildString {
        append("monero:$address")
        val params = mutableListOf<String>()
        amount?.let { params.add("tx_amount=$it") }
        recipientName?.let { params.add("recipient_name=${URLEncoder.encode(it, "UTF-8")}") }
        if (params.isNotEmpty()) {
            append("?${params.joinToString("&")}")
        }
    }
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback: suggest wallet installation
        onError("No Monero wallet found. Install Cake Wallet or Monerujo.")
    }
}
```

### 5. Profile Header Integration

**Modify:** `amethyst/src/main/java/com/vitorpamplona/amethyst/ui/screen/loggedIn/profile/header/DrawAdditionalInfo.kt`

```kotlin
@Composable
fun DrawAdditionalInfo(...) {
    // ... existing code ...
    
    // Lightning Address (existing)
    DisplayLNAddress(user.info.lud16, baseUser, accountViewModel, nav)
    
    // NEW: XMR Address
    DisplayXMRAddress(
        xmrAddress = user.info.xmr,
        xmrOpenAlias = user.info.xmrOpenAlias,
        user = baseUser,
        accountViewModel = accountViewModel,
        nav = nav
    )
}
```

### 6. Profile Edit Screen

**Modify:** Add XMR address field to profile edit screen

```kotlin
// In profile edit form
OutlinedTextField(
    value = xmrAddress,
    onValueChange = { xmrAddress = it },
    label = { Text("Monero Address or OpenAlias") },
    placeholder = { Text("4... or user@domain.com") },
    leadingIcon = { MoneroIcon() }
)
```

---

## UI Assets

### Monero Icon
- Orange color: `#FF6600`
- SVG icon for Monero logo (similar to Lightning icon)

### Colors
```kotlin
val MoneroOrange = Color(0xFFFF6600)
```

---

## OpenAlias Specification

**DNS TXT Record Format:**
```
oa1:xmr recipient_address=<XMR_ADDRESS>; recipient_name=<NAME>; tx_description=<DESC>;
```

**Example:**
```
TXT donate.getmonero.org
"oa1:xmr recipient_address=888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H; recipient_name=Monero Development; tx_description=Donation to Monero Core Team;"
```

**Resolution Flow:**
1. User enters `donate@getmonero.org`
2. Query DNS TXT for `donate.getmonero.org`
3. Parse `oa1:xmr` record
4. Extract `recipient_address` value
5. Display/use the resolved XMR address

---

## Compatible Wallets

Deep-link URI scheme `monero:` is supported by:
- **Cake Wallet** (iOS/Android)
- **Monerujo** (Android)
- **Feather Wallet** (Desktop)
- **MyMonero** (iOS/Android/Desktop)

---

## Implementation Phases

### Phase 1: Core (MVP)
- [ ] Add XMR/xmrOpenAlias fields to UserMetadata
- [ ] Create OpenAliasResolver
- [ ] Create DisplayXMRAddress component
- [ ] Add to profile header

### Phase 2: UX Polish
- [ ] Profile edit screen integration
- [ ] XMR tip amount dialog (like Lightning)
- [ ] Address validation (4... for standard, 8... for subaddress)
- [ ] Copy address to clipboard

### Phase 3: Advanced
- [ ] DNSSEC validation warning (like OpenAlias spec recommends)
- [ ] Multiple crypto support (oa1:btc already exists in OpenAlias)
- [ ] QR code display for XMR address

---

## Testing

### OpenAlias Test Addresses
- `donate@getmonero.org` — Monero Core Team
- Known working example for integration testing

### XMR Address Validation
- Standard: starts with `4`, 95 characters
- Subaddress: starts with `8`, 95 characters
- Integrated: starts with `4`, 106 characters

---

## References

- [OpenAlias Specification](https://openalias.org/)
- [Monero URI Scheme](https://github.com/monero-project/monero/wiki/URI-Formatting)
- [Amethyst Source](https://github.com/vitorpamplona/amethyst)
- [NIP-01 Metadata](https://github.com/nostr-protocol/nips/blob/master/01.md)
