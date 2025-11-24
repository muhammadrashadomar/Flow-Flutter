# Payment Integration - Quick Reference

## Flutter (Dart) API

### Initialize Payment Bridge

```dart
final paymentBridge = PaymentBridge();
paymentBridge.initialize();
```

### Set Up Callbacks

```dart
paymentBridge.onCardTokenized = (result) {
  print('✅ Token: ${result.token}');
  print('Last 4: ${result.last4}');
  print('Brand: ${result.brand}');
  print('Expiry: ${result.expiryMonth}/${result.expiryYear}');
};

paymentBridge.onPaymentSuccess = (result) {
  print('✅ Payment ID: ${result.paymentId}');
};

paymentBridge.onPaymentError = (result) {
  print('❌ Error: ${result.errorCode} - ${result.errorMessage}');
};
```

### Card Payment

```dart
// 1. Create configuration
final config = PaymentConfig(
  paymentSessionId: "ps_xxx",
  paymentSessionSecret: "pss_xxx",
  publicKey: "pk_sbox_xxx",
  environment: PaymentEnvironment.sandbox,
);

final cardConfig = CardConfig(
  showCardholderName: false,
);

// 2. Initialize
await paymentBridge.initCardView(config, cardConfig);

// 3. Render card input
AndroidView(
  viewType: 'flow_card_view',
  creationParams: config.toMap(),
  creationParamsCodec: const StandardMessageCodec(),
)

// 4. Add Flutter button
ElevatedButton(
  onPressed: () async {
    // Validate first
    bool valid = await paymentBridge.validateCard();
    if (valid) {
      await paymentBridge.tokenizeCard();
    }
  },
  child: Text('Pay Now'),
)
```

### Google Pay

```dart
// 1. Create configuration
final googlePayConfig = GooglePayConfig(
  merchantId: "your_merchant_id",
  merchantName: "Your Business",
  countryCode: "US",
  currencyCode: "USD",
  totalPrice: 1000, // in cents
);

// 2. Initialize
await paymentBridge.initGooglePay(config, googlePayConfig);

// 3. Check availability
bool available = await paymentBridge.checkGooglePayAvailability();

// 4. Launch payment sheet
if (available) {
  await paymentBridge.launchGooglePaySheet({
    'amount': 1000,
    'currency': 'USD',
  });
}
```

---

## Android (Kotlin) API

### MainActivity Setup

```kotlin
class MainActivity : FlutterFragmentActivity() {
    private val CHANNEL = "checkout_bridge"
    private var cardPlatformView: CardPlatformView? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val messenger = flutterEngine.dartExecutor.binaryMessenger
        val registry = flutterEngine.platformViewsController.registry

        // Register with callback
        registry.registerViewFactory(
            "flow_card_view",
            CardViewFactory(messenger, this) { view ->
                cardPlatformView = view
            }
        )

        // Set up method channel
        MethodChannel(messenger, CHANNEL).setMethodCallHandler { call, result ->
            handleMethodCall(call, result)
        }
    }
}
```

### Method Channel Handler

```kotlin
private fun handleMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
        "validateCard" -> {
            val isValid = cardPlatformView?.validateCard() ?: false
            result.success(isValid)
        }
        "tokenizeCard" -> {
            cardPlatformView?.tokenizeCard(result)
        }
        else -> result.notImplemented()
    }
}
```

### Send Events to Flutter

```kotlin
// In CardPlatformView.kt

// Success
channel.invokeMethod("cardTokenized", mapOf(
    "tokenDetails" to tokenData
))

// Error
channel.invokeMethod("paymentError", mapOf(
    "code" to "ERROR_CODE",
    "message" to "Error message"
))
```

---

## Method Channel Contract

### Flutter → Android

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `initCardView` | `PaymentConfig`, `CardConfig` | `bool` | Initialize card component |
| `validateCard` | - | `bool` | Check if card input is valid |
| `tokenizeCard` | - | `Map` | Trigger tokenization |
| `initGooglePay` | `PaymentConfig`, `GooglePayConfig` | `bool` | Initialize Google Pay |
| `checkGooglePayAvailability` | - | `bool` | Check device support |
| `launchGooglePaySheet` | `Map<String, Any>` | `Map` | Launch payment sheet |

### Android → Flutter (Callbacks)

| Method | Parameters | Description |
|--------|-----------|-------------|
| `cardTokenized` | `Map<String, dynamic>` | Card successfully tokenized |
| `paymentSuccess` | `String` (paymentId) | Payment completed |
| `paymentError` | `Map<String, dynamic>` | Error occurred |

---

## Configuration Reference

### PaymentConfig

```dart
PaymentConfig(
  paymentSessionId: String,      // Required: Session ID from backend
  paymentSessionSecret: String,   // Required: Session secret
  publicKey: String,              // Required: Checkout.com public key
  environment: PaymentEnvironment, // sandbox | production
  appearance: AppearanceConfig?,  // Optional: UI customization
)
```

### AppearanceConfig

```dart
AppearanceConfig(
  borderRadius: int?,           // Border radius for inputs/buttons
  colorTokens: ColorTokens?,    // Color customization
  fontConfig: FontConfig?,      // Font customization
)

ColorTokens(
  colorAction: int?,      // Primary action color (hex: 0xFFRRGGBB)
  colorPrimary: int?,     // Primary text color
  colorBorder: int?,      // Border color
  colorFormBorder: int?,  // Form field border
  colorBackground: int?,  // Background color
)
```

### CardConfig

```dart
CardConfig(
  showCardholderName: bool,    // Show cardholder name field
  enableBillingAddress: bool,  // Enable billing address
)
```

---

## Error Codes

| Code | Description | Action |
|------|-------------|--------|
| `CARD_NOT_READY` | Card component not initialized | Call `initCardView()` first |
| `CARD_NOT_AVAILABLE` | Card payment method unavailable | Check session config |
| `INIT_ERROR` | Initialization failed | Check credentials and logs |
| `TOKEN_ERROR` | Tokenization failed | Validate card input |
| `CHECKOUT_ERROR` | Checkout.com SDK error | Check SDK logs |
| `GOOGLEPAY_NOT_READY` | Google Pay not initialized | Call `initGooglePay()` first |
| `GOOGLEPAY_NOT_AVAILABLE` | Google Pay not supported | Check device/account |

---

## Environment Values

```dart
// Sandbox (Testing)
environment: PaymentEnvironment.sandbox,

// Production (Live)
environment: PaymentEnvironment.production,
```

**Important:** Ensure your credentials match the environment!

---

## Color Format

Colors use ARGB hex format:

```dart
0xFFRRGGBB
│ │ │ │
│ │ │ └─ Blue (00-FF)
│ │ └─── Green (00-FF)
│ └───── Red (00-FF)
└─────── Alpha (FF = opaque)

Examples:
0XFF00639E  // Blue
0XFF111111  // Dark gray
0XFFCCCCCC  // Light gray
0xFFFFFFFF  // White
```

---

## Common Patterns

### Show Card Sheet

```dart
void showCardSheet(BuildContext context) {
  showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    builder: (_) => FractionallySizedBox(
      heightFactor: 0.65,
      child: Column(
        children: [
          Expanded(
            child: AndroidView(
              viewType: 'flow_card_view',
              creationParams: config.toMap(),
              creationParamsCodec: const StandardMessageCodec(),
            ),
          ),
          ElevatedButton(
            onPressed: () => paymentBridge.tokenizeCard(),
            child: Text('Pay Now'),
          ),
        ],
      ),
    ),
  );
}
```

### Handle Loading State

```dart
bool _isProcessing = false;

Future<void> _tokenizeCard() async {
  setState(() => _isProcessing = true);
  
  try {
    await _paymentBridge.tokenizeCard();
    // Result comes via onCardTokenized callback
  } catch (e) {
    setState(() => _isProcessing = false);
    // Show error
  }
}

// In UI
ElevatedButton(
  onPressed: _isProcessing ? null : _tokenizeCard,
  child: _isProcessing 
    ? CircularProgressIndicator()
    : Text('Pay Now'),
)
```

---

## Debug Logging

### Flutter

```dart
// In PaymentBridge
print('📱 Received method call: ${call.method}');
print('✅ Card tokenized: $result');
print('❌ Payment error: $result');
```

### Android

```kotlin
// In CardPlatformView
Log.d("CardPlatformView", "Starting tokenization...")
Log.e("CardPlatformView", "Error: ${e.message}")
```

---

## Quick Troubleshooting

| Problem | Check |
|---------|-------|
| Card view not showing | - Valid credentials?<br>- `initCardView()` called?<br>- Check Android logs |
| Tokenization fails | - Card input valid?<br>- Component initialized?<br>- Check callbacks set |
| Callbacks not firing | - `initialize()` called?<br>- Channel name correct?<br>- Listeners set before actions? |
| Colors not applying | - Correct ARGB format?<br>- `appearance` passed in config?<br>- Check Android parsing |

---

## Performance Tips

1. **Initialize once**: Don't recreate PaymentBridge multiple times
2. **Dispose properly**: Call `dispose()` in State lifecycle
3. **Reuse configs**: Cache PaymentConfig if possible
4. **Async carefully**: Use `async/await` properly for method calls
5. **Error handling**: Always wrap calls in try-catch

---

## Security Best Practices

1. ✅ Never hardcode session secrets
2. ✅ Fetch session credentials from your backend
3. ✅ Use HTTPS for all API calls
4. ✅ Don't log sensitive data in production
5. ✅ Validate environment matches credentials
6. ✅ Never store tokens unencrypted

---

**For detailed architecture, see `ARCHITECTURE.md`**

**For migration guide, see `MIGRATION_GUIDE.md`**
