# Payment Integration - Refactored Architecture

[![Flutter](https://img.shields.io/badge/Flutter-3.0+-02569B?logo=flutter)](https://flutter.dev)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Checkout.com](https://img.shields.io/badge/Checkout.com-SDK-00D632)](https://www.checkout.com)

A production-ready Flutter payment integration with clean architecture, featuring card tokenization and Google Pay support via Checkout.com SDK.

## ✨ Features

- 🎯 **Card Tokenization** - Tokenize cards directly from Flutter button
- 💳 **Google Pay** - Native Google Pay sheet integration
- 🎨 **Customizable UI** - Full control over appearance from Flutter
- 🔧 **Dynamic Configuration** - No hardcoded values in native code
- 🔒 **Secure** - Best practices for payment data handling
- 🧪 **Production Ready** - Comprehensive error handling and logging
- 📱 **Clean Architecture** - Clear separation of concerns

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────┐
│         Flutter Layer (Dart)         │
├─────────────────────────────────────┤
│  • PaymentBridge (Service)           │
│  • PaymentConfig (Models)            │
│  • PaymentResult (Models)            │
│  • UI Components                     │
└────────────┬────────────────────────┘
             │ Method Channel
             │ (checkout_bridge)
┌────────────┴────────────────────────┐
│      Android Native Layer (Kotlin)   │
├─────────────────────────────────────┤
│  • MainActivity (Method Handler)     │
│  • CardPlatformView (Card Input)     │
│  • GooglePayPlatformView (GPay)      │
│  • Checkout.com SDK Integration      │
└─────────────────────────────────────┘
```

**Key Principle**: Flutter controls everything, native provides input components only.

## 🚀 Quick Start

### 1. Add Dependencies

```yaml
# pubspec.yaml
dependencies:
  flutter:
    sdk: flutter
```

### 2. Initialize Payment Bridge

```dart
import 'services/payment_bridge.dart';
import 'models/payment_config.dart';

final paymentBridge = PaymentBridge();
paymentBridge.initialize();

// Set up callbacks
paymentBridge.onCardTokenized = (result) {
  print('Token: ${result.token}');
};
```

### 3. Configure Payment

```dart
final config = PaymentConfig(
  paymentSessionId: "ps_xxx",
  paymentSessionSecret: "pss_xxx",
  publicKey: "pk_sbox_xxx",
  environment: PaymentEnvironment.sandbox,
);

final cardConfig = CardConfig(
  showCardholderName: false,
);

await paymentBridge.initCardView(config, cardConfig);
```

### 4. Display Card Input & Button

```dart
Column(
  children: [
    // Native card input
    Expanded(
      child: AndroidView(
        viewType: 'flow_card_view',
        creationParams: config.toMap(),
        creationParamsCodec: const StandardMessageCodec(),
      ),
    ),
    // Flutter button
    ElevatedButton(
      onPressed: () => paymentBridge.tokenizeCard(),
      child: Text('Pay Now'),
    ),
  ],
)
```

## 📱 Flutter API

### PaymentBridge Methods

| Method | Description |
|--------|-------------|
| `initialize()` | Initialize payment bridge |
| `initCardView(config, cardConfig)` | Initialize card component |
| `validateCard()` | Validate card input |
| `tokenizeCard()` | Trigger tokenization |
| `initGooglePay(config, googlePayConfig)` | Initialize Google Pay |
| `checkGooglePayAvailability()` | Check if Google Pay is available |
| `launchGooglePaySheet(requestData)` | Launch Google Pay sheet |
| `dispose()` | Clean up resources |

### Callbacks

```dart
paymentBridge.onCardTokenized = (CardTokenResult result) { };
paymentBridge.onPaymentSuccess = (PaymentSuccessResult result) { };
paymentBridge.onPaymentError = (PaymentErrorResult result) { };
```

## 🔧 Configuration

### Payment Configuration

```dart
PaymentConfig(
  paymentSessionId: "ps_xxx",      // From your backend
  paymentSessionSecret: "pss_xxx",  // From your backend
  publicKey: "pk_sbox_xxx",         // Checkout.com public key
  environment: PaymentEnvironment.sandbox,
  appearance: AppearanceConfig(
    borderRadius: 8,
    colorTokens: ColorTokens(
      colorAction: 0XFF00639E,
      colorPrimary: 0XFF111111,
      colorBorder: 0XFFCCCCCC,
    ),
  ),
)
```

## 🎨 Customization

Fully customize the appearance from Flutter:

```dart
AppearanceConfig(
  borderRadius: 12,
  colorTokens: ColorTokens(
    colorAction: 0XFF4CAF50,      // Primary action color
    colorPrimary: 0XFF212121,     // Text color
    colorBorder: 0XFFE0E0E0,      // Border color
    colorFormBorder: 0XFF9E9E9E,  // Form field border
  ),
)
```

## 📚 Documentation

- **[Architecture Guide](ARCHITECTURE.md)** - Detailed architecture documentation
- **[Migration Guide](MIGRATION_GUIDE.md)** - Migrate from old architecture
- **[Quick Reference](QUICK_REFERENCE.md)** - Code snippets and examples

## 🔒 Security

- ✅ No hardcoded credentials
- ✅ Session secrets from backend only
- ✅ Proper error handling
- ✅ No sensitive data in logs (production)
- ✅ Token data encrypted in transit

## 🐛 Troubleshooting

### Card view not showing

```
✓ Check session credentials are valid
✓ Verify initCardView() was called
✓ Check Android logs for errors
```

### Tokenization fails

```
✓ Ensure card input is valid
✓ Verify component is initialized
✓ Check callbacks are set
```

### Google Pay not available

```
✓ Check device supports Google Pay
✓ Verify Google Play Services installed
✓ Check merchant configuration
```

See [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for more.

## 📝 Example

Complete example in `lib/main.dart`:

```dart
class PaymentScreen extends StatefulWidget {
  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  final PaymentBridge _paymentBridge = PaymentBridge();
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _setupPaymentBridge();
  }

  void _setupPaymentBridge() {
    _paymentBridge.initialize();
    
    _paymentBridge.onCardTokenized = (result) {
      setState(() => _isProcessing = false);
      _showSuccess('Token: ${result.token}');
    };
    
    _paymentBridge.onPaymentError = (result) {
      setState(() => _isProcessing = false);
      _showError(result.errorMessage);
    };
  }

  Future<void> _tokenizeCard() async {
    setState(() => _isProcessing = true);
    await _paymentBridge.tokenizeCard();
  }

  @override
  Widget build(BuildContext context) {
    // ... UI implementation
  }
}
```

## 🎯 Key Improvements Over Previous Architecture

| Aspect | Before | After |
|--------|--------|-------|
| Configuration | ❌ Hardcoded in Android | ✅ Dynamic from Flutter |
| Button Control | ❌ Native button | ✅ Flutter button |
| API | ❌ Scattered methods | ✅ Clean PaymentBridge |
| Results | ❌ Raw callbacks | ✅ Typed models |
| Errors | ❌ Inconsistent | ✅ Structured with codes |
| Testing | ❌ Difficult | ✅ Easy to test |
| Maintenance | ❌ Hard to modify | ✅ Easy to extend |

## 🛠️ Tech Stack

- **Flutter** - UI framework
- **Kotlin** - Android native code
- **Checkout.com SDK** - Payment processing
- **Method Channels** - Platform communication

## 📦 Project Structure

```
lib/
├── models/
│   ├── payment_config.dart      # Configuration models
│   └── payment_result.dart      # Result models
├── services/
│   └── payment_bridge.dart      # Unified payment bridge
└── main.dart                     # UI implementation

android/app/src/main/kotlin/com/example/flow_flutter_new/
├── MainActivity.kt               # Method channel handler
├── CardPlatformView.kt          # Card input component
├── GooglePayPlatformView.kt     # Google Pay component
└── views/
    ├── CardViewFactory.kt
    └── GooglePayViewFactory.kt
```

## 🤝 Contributing

This is a refactored production architecture. Key principles when extending:

1. Keep Flutter in control
2. Native provides components only
3. All config from Flutter
4. Structured error handling
5. Comprehensive logging

## 📄 License

This is an internal project. Refer to your organization's license.

## 💬 Support

For issues:
- **Architecture questions**: See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Code examples**: See [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- **Migration**: See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)

---

**Built with ❤️ using clean architecture principles**