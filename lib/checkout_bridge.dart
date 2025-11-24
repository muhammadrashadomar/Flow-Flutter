import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

enum CallMethod { paymentSuccess, paymentError, cardTokenized, getSessionData }

class CheckoutBridge {
  static const MethodChannel _channel = MethodChannel('checkout_bridge');

  // ✅ Method to Listen for Payment Results from iOS
  static void listenForPaymentResults(BuildContext context) {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == CallMethod.paymentSuccess.name) {
        _showPaymentDialog(
          context,
          "Payment Successful",
          "Payment ID: ${call.arguments}",
        );
      } else if (call.method == CallMethod.paymentError.name) {
        _showPaymentDialog(
          context,
          "Payment Failed",
          "Error: ${call.arguments}",
        );
      } else if (call.method == CallMethod.cardTokenized.name) {
        _showPaymentDialog(
          context,
          "Card Tokenized",
          "Token: ${call.arguments}",
        );
      } else if (call.method == CallMethod.getSessionData.name) {
        _showPaymentDialog(
          context,
          "Session Data",
          "Session Data: ${call.arguments}",
        );
      }
    });
  }

  // ✅ Function to Show Dialog
  static void _showPaymentDialog(
    BuildContext context,
    String title,
    String message,
  ) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text("OK"),
            ),
          ],
        );
      },
    );
  }
}
