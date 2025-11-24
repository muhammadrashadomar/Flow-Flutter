import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'models/payment_config.dart';
import 'models/payment_result.dart';
import 'services/payment_bridge.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Payment Integration',
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
      home: const PaymentScreen(),
    );
  }
}

class PaymentScreen extends StatefulWidget {
  const PaymentScreen({super.key});

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  final PaymentBridge _paymentBridge = PaymentBridge();

  bool _isProcessing = false;
  bool _cardInitialized = false;

  // Payment configuration
  final PaymentConfig _paymentConfig = PaymentConfig(
    paymentSessionId: "ps_35vKy0c5IJS4m9AdrgMn6mlB9jY",
    paymentSessionSecret: "pss_f818d31e-c1b3-4e23-a76e-abd37086244a",
    publicKey: "pk_sbox_fjizign6afqbt3btt3ialiku74s",
    environment: PaymentEnvironment.sandbox,
    appearance: AppearanceConfig(
      borderRadius: 8,
      colorTokens: ColorTokens(
        colorAction: 0XFF00639E,
        colorPrimary: 0XFF111111,
        colorBorder: 0XFFCCCCCC,
        colorFormBorder: 0XFFCCCCCC,
      ),
    ),
  );

  @override
  void initState() {
    super.initState();
    _setupPaymentBridge();
  }

  void _setupPaymentBridge() {
    _paymentBridge.initialize();

    // Set up payment callbacks
    _paymentBridge.onCardTokenized = _handleCardTokenized;
    _paymentBridge.onPaymentSuccess = _handlePaymentSuccess;
    _paymentBridge.onPaymentError = _handlePaymentError;
    _paymentBridge.onSessionData = _handleSessionData;
  }

  void _handleCardTokenized(CardTokenResult result) {
    setState(() => _isProcessing = false);
    _showResultDialog(
      'Card Tokenized',
      'Token: ${result.token}\n'
          'Last 4: ${result.last4 ?? 'N/A'}\n'
          'Brand: ${result.brand ?? 'N/A'}\n'
          'Expiry: ${result.expiryMonth ?? 'N/A'}/${result.expiryYear ?? 'N/A'}',
      isSuccess: true,
    );
  }

  void _handleSessionData(String sessionData) {
    setState(() => _isProcessing = false);
    _showResultDialog(
      'Session Data',
      'Session Data: $sessionData',
      isSuccess: true,
    );
  }

  void _handlePaymentSuccess(PaymentSuccessResult result) {
    setState(() => _isProcessing = false);
    _showResultDialog(
      'Payment Successful',
      'Payment ID: ${result.paymentId}',
      isSuccess: true,
    );
  }

  void _handlePaymentError(PaymentErrorResult result) {
    setState(() => _isProcessing = false);
    _showResultDialog(
      'Payment Error',
      '${result.errorCode}: ${result.errorMessage}',
      isSuccess: false,
    );
  }

  void _showResultDialog(
    String title,
    String message, {
    required bool isSuccess,
  }) {
    showDialog(
      context: context,
      builder:
          (_) => AlertDialog(
            title: Row(
              children: [
                Icon(
                  isSuccess ? Icons.check_circle : Icons.error,
                  color: isSuccess ? Colors.green : Colors.red,
                ),
                const SizedBox(width: 8),
                Text(title),
              ],
            ),
            content: Text(message),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('OK'),
              ),
            ],
          ),
    );
  }

  Future<void> _showCardSheet() async {
    // Initialize card view first if not done
    if (!_cardInitialized) {
      final cardConfig = CardConfig(
        showCardholderName: false,
        enableBillingAddress: false,
      );
      _cardInitialized = await _paymentBridge.initCardView(
        _paymentConfig,
        cardConfig,
      );
    }

    if (!mounted) return;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder:
          (_) => _CardBottomSheet(
            paymentConfig: _paymentConfig,
            onTokenize: _tokenizeCard,
            isProcessing: _isProcessing,
          ),
    );
  }

  Future<void> _tokenizeCard() async {
    setState(() => _isProcessing = true);

    // Validate card first
    final isValid = await _paymentBridge.validateCard();

    if (!isValid) {
      setState(() => _isProcessing = false);
      _showResultDialog(
        'Validation Error',
        'Please check your card details',
        isSuccess: false,
      );
      return;
    }

    // Trigger tokenization
    await _paymentBridge.tokenizeCard();
  }

  Future<void> _getSessionData() async {
    setState(() => _isProcessing = true);
    await _paymentBridge.submit();
  }

  void _selectMethod(String method) {
    switch (method) {
      case 'card':
        Future.delayed(const Duration(milliseconds: 100), _showCardSheet);
        break;
      case 'googlepay':
        _showGooglePayView();
        break;
    }
  }

  void _showGooglePayView() {
    // For now, show existing Google Pay view
    // In future, this should use flutter_pay package
    showDialog(
      context: context,
      builder:
          (_) => AlertDialog(
            title: const Text('Google Pay'),
            content: const Text(
              'Google Pay integration will use flutter_pay package.\n\n'
              'Native code will only expose the payment sheet logic.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('OK'),
              ),
            ],
          ),
    );
  }

  @override
  void dispose() {
    _paymentBridge.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade100,
      appBar: AppBar(
        title: const Text('Payment Methods'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 16),
            _PaymentMethodButton(
              label: 'Pay with Card',
              icon: Icons.credit_card,
              color: Colors.blue,
              onPressed: () => _selectMethod('card'),
            ),
            const SizedBox(height: 12),
            _PaymentMethodButton(
              label: 'Pay with Google Pay',
              icon: Icons.payment,
              color: Colors.black,
              onPressed: () => _selectMethod('googlepay'),
            ),

            const SizedBox(height: 60),
            _PaymentMethodButton(
              label: 'Submit',
              icon: Icons.payment,
              color: Colors.blueGrey,
              onPressed: () => _getSessionData(),
            ),
          ],
        ),
      ),
    );
  }
}

class _PaymentMethodButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onPressed;

  const _PaymentMethodButton({
    required this.label,
    required this.icon,
    required this.color,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon),
          const SizedBox(width: 12),
          Text(
            label,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _CardBottomSheet extends StatelessWidget {
  final PaymentConfig paymentConfig;
  final VoidCallback onTokenize;
  final bool isProcessing;

  const _CardBottomSheet({
    required this.paymentConfig,
    required this.onTokenize,
    required this.isProcessing,
  });

  @override
  Widget build(BuildContext context) {
    return FractionallySizedBox(
      heightFactor: 0.65,
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          children: [
            // Handle bar
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),

            // Title
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                'Enter Card Details',
                style: Theme.of(context).textTheme.titleLarge,
              ),
            ),

            // Card input widget (platform view)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: _PlatformCardView(paymentConfig: paymentConfig),
                ),
              ),
            ),

            // Pay button
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: isProcessing ? null : onTokenize,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF00639E),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    disabledBackgroundColor: Colors.grey.shade300,
                  ),
                  child:
                      isProcessing
                          ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                Colors.white,
                              ),
                            ),
                          )
                          : const Text(
                            'Tokenize',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PlatformCardView extends StatelessWidget {
  final PaymentConfig paymentConfig;

  const _PlatformCardView({required this.paymentConfig});

  @override
  Widget build(BuildContext context) {
    const viewType = 'flow_card_view';
    final creationParams = paymentConfig.toMap();

    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: viewType,
        creationParams: creationParams,
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: 'flow_view_card',
        creationParams: creationParams,
        creationParamsCodec: const StandardMessageCodec(),
      );
    }

    return const Center(child: Text('Platform not supported'));
  }
}
