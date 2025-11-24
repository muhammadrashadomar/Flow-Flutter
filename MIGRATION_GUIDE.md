# Migration Guide: Old Architecture → New Architecture

## Overview

This guide helps you migrate from the old payment integration to the new refactored architecture.

---

## Key Changes Summary

| Aspect | Old Architecture | New Architecture |
|--------|-----------------|------------------|
| **Configuration** | Hardcoded in Android | Dynamic from Flutter via models |
| **Button Control** | Native Pay button | Flutter button only |
| **Method Calls** | Scattered, unclear | Clean `PaymentBridge` API |
| **Results** | Direct callbacks to UI | Structured result models |
| **Error Handling** | Inconsistent | Standardized with error codes |
| **State Management** | Mixed | Clear separation of concerns |

---

## Step-by-Step Migration

### Step 1: Update Imports

**Old:**
```dart
import 'checkout_bridge.dart';
```

**New:**
```dart
import 'services/payment_bridge.dart';
import 'models/payment_config.dart';
import 'models/payment_result.dart';
```

---

### Step 2: Replace CheckoutBridge with PaymentBridge

**Old:**
```dart
// In initState
CheckoutBridge.listenForPaymentResults(context);
```

**New:**
```dart
// In initState
final paymentBridge = PaymentBridge();
paymentBridge.initialize();

paymentBridge.onCardTokenized = (result) {
  print('Token: ${result.token}');
};

paymentBridge.onPaymentSuccess = (result) {
  print('Payment ID: ${result.paymentId}');
};

paymentBridge.onPaymentError = (result) {
  print('Error: ${result.errorMessage}');
};
```

---

### Step 3: Update Configuration

**Old:**
```dart
const sessionParams = {
  'paymentSessionID': "ps_xxx",
  'paymentSessionSecret': "pss_xxx",
  'publicKey': "pk_sbox_xxx",
};
```

**New:**
```dart
final paymentConfig = PaymentConfig(
  paymentSessionId: "ps_xxx",
  paymentSessionSecret: "pss_xxx",
  publicKey: "pk_sbox_xxx",
  environment: PaymentEnvironment.sandbox,
  appearance: AppearanceConfig(
    borderRadius: 8,
    colorTokens: ColorTokens(
      colorAction: 0XFF00639E,
      colorPrimary: 0XFF111111,
    ),
  ),
);
```

---

### Step 4: Update Card View Usage

**Old:**
```dart
AndroidView(
  viewType: 'flow_card_view',
  creationParams: sessionParams,
  creationParamsCodec: const StandardMessageCodec(),
)
```

**New:**
```dart
// Initialize first
final cardConfig = CardConfig(
  showCardholderName: false,
);
await paymentBridge.initCardView(paymentConfig, cardConfig);

// Then use in widget tree
AndroidView(
  viewType: 'flow_card_view',
  creationParams: paymentConfig.toMap(),
  creationParamsCodec: const StandardMessageCodec(),
)
```

---

### Step 5: Replace Native Button with Flutter Button

**Old:**
```dart
// Card component had built-in button
AndroidView(
  viewType: 'flow_card_view',
  // Button automatically shown
)
```

**New:**
```dart
Column(
  children: [
    // Card input (no button)
    Expanded(
      child: AndroidView(
        viewType: 'flow_card_view',
        creationParams: paymentConfig.toMap(),
        creationParamsCodec: const StandardMessageCodec(),
      ),
    ),
    // Flutter button
    ElevatedButton(
      onPressed: () async {
        await paymentBridge.tokenizeCard();
      },
      child: Text('Pay Now'),
    ),
  ],
)
```

---

### Step 6: Update Google Pay Integration

**Old:**
```dart
// Used native Google Pay button
AndroidView(
  viewType: 'flow_googlepay_view',
  // Included Google Pay button
)
```

**New Option A** (Using existing integration):
```dart
// Initialize
await paymentBridge.initGooglePay(paymentConfig, googlePayConfig);

// Check availability
bool available = await paymentBridge.checkGooglePayAvailability();

// Launch sheet
await paymentBridge.launchGooglePaySheet(requestData);
```

**New Option B** (Recommended - use flutter_pay):
```dart
import 'package:flutter_pay/flutter_pay.dart';

// Check availability
bool available = await FlutterPay.canMakePayments();

// Launch Google Pay
await FlutterPay.makePayment(paymentItems);
```

---

### Step 7: Update Android Code

**MainActivity.kt Old:**
```kotlin
// Multiple method channel handlers
registry.registerViewFactory("flow_card_view", CardViewFactory(messenger, this))

MethodChannel(messenger, CHANNEL).setMethodCallHandler { call, result ->
    when (call.method) {
        "tokenizeCard" -> {
            if (!::cardView.isInitialized) { ... }
            cardView.tokenizeCard(result)
        }
    }
}
```

**MainActivity.kt New:**
```kotlin
// Factory with callback
registry.registerViewFactory(
    "flow_card_view",
    CardViewFactory(messenger, this) { view ->
        cardPlatformView = view
    }
)

// Single handler
MethodChannel(messenger, CHANNEL).setMethodCallHandler { call, result ->
    handleMethodCall(call, result)
}
```

---

### Step 8: Update CardPlatformView

**Old CardPlatformView:**
- Configuration hardcoded
- Native pay button included
- Inconsistent error handling

**New CardPlatformView:**
- All configuration from Flutter params
- `showPayButton = false`
- Structured error handling with codes
- Proper logging
- Thread-safe callbacks

**Required Changes:**
1. Remove hardcoded design tokens
2. Parse configuration from params
3. Hide native pay button
4. Use structured error callbacks

---

## Breaking Changes

### 1. Method Channel Names

| Old | New |
|-----|-----|
| `CheckoutBridge` class | `PaymentBridge` class |
| Direct context callbacks | Callback properties |

### 2. Callback Signatures

**Old:**
```dart
static void listenForPaymentResults(BuildContext context)
```

**New:**
```dart
paymentBridge.onCardTokenized = (CardTokenResult result) { };
paymentBridge.onPaymentSuccess = (PaymentSuccessResult result) { };
paymentBridge.onPaymentError = (PaymentErrorResult result) { };
```

### 3. Configuration Format

**Old:** Simple maps
**New:** Typed configuration objects

### 4. Native Button

**Old:** Included in card view
**New:** Must be added separately in Flutter

---

## Testing Checklist

After migration, verify:

- [ ] Card input displays correctly
- [ ] Card validation works
- [ ] Tokenization triggered by Flutter button
- [ ] Token result received with all fields
- [ ] Payment success callback works
- [ ] Error handling displays correctly
- [ ] Google Pay availability check works
- [ ] Google Pay sheet launches
- [ ] Different environments work (sandbox/production)
- [ ] Custom appearance applies correctly
- [ ] Lifecycle (dispose) handled properly

---

## Common Issues & Solutions

### Issue: Card view not displaying

**Solution:**
- Check `initCardView()` was called before rendering
- Verify session credentials are valid
- Check Android logs for initialization errors

### Issue: "Card component not initialized" error

**Solution:**
- Ensure platform view is created before calling methods
- Add delay or wait for initialization callback
- Check factory callback is capturing view instance

### Issue: Callbacks not firing

**Solution:**
- Verify `paymentBridge.initialize()` was called
- Check method channel name matches: `"checkout_bridge"`
- Ensure callbacks set before triggering actions

### Issue: Type errors in result handling

**Solution:**
- Use provided result models (`CardTokenResult`, etc.)
- Don't cast directly, use `.fromMap()` factories
- Check result data structure in logs

---

## Rollback Plan

If you need to rollback:

1. Keep old files:
   - `checkout_bridge.dart.backup`
   - `MainActivity.kt.backup`
   - `CardPlatformView.kt.backup`

2. Restore from backups

3. Revert `main.dart` to use old pattern

4. Remove new files:
   - `lib/models/`
   - `lib/services/payment_bridge.dart`

---

## Getting Help

1. **Architecture Questions**: Read `ARCHITECTURE.md`
2. **Code Examples**: Check `lib/main.dart` for complete implementation
3. **Android Issues**: Review `CardPlatformView.kt` comments
4. **Method Channel**: See method channel contract in `ARCHITECTURE.md`

---

## Timeline Recommendation

- **Day 1**: Update models and PaymentBridge
- **Day 2**: Migrate Flutter UI layer
- **Day 3**: Update Android native code
- **Day 4**: Testing and fixes
- **Day 5**: Documentation and cleanup

---

## Success Metrics

Migration is successful when:

1. ✅ All tests pass
2. ✅ Code is more maintainable
3. ✅ No hardcoded values in native
4. ✅ Clear separation of concerns
5. ✅ Easy to add new features
6. ✅ Better error handling
7. ✅ Production-ready code

---

## Next Steps After Migration

1. Add comprehensive tests
2. Implement proper state management (Bloc/Riverpod)
3. Add analytics tracking
4. Implement 3DS flow
5. Add stored payment methods
6. Consider removing GooglePayPlatformView and using `flutter_pay`

---

**Good luck with your migration! 🚀**
