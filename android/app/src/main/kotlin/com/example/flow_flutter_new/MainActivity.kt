package com.example.flow_flutter_new

import android.util.Log
import com.example.flow_flutter_new.views.CardViewFactory
import com.example.flow_flutter_new.views.GooglePayViewFactory
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformViewRegistry

/**
 * Main Activity - Handles platform view registration and method channel setup Follows clean
 * architecture with separation of concerns
 */
class MainActivity : FlutterFragmentActivity() {
    private val CHANNEL = "checkout_bridge"
    private var cardPlatformView: CardPlatformView? = null
    private var googlePayPlatformView: GooglePayPlatformView? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val messenger = flutterEngine.dartExecutor.binaryMessenger
        val registry: PlatformViewRegistry = flutterEngine.platformViewsController.registry

        // Register platform views with callback to capture instances
        registry.registerViewFactory(
                "flow_card_view",
                CardViewFactory(messenger, this) { view ->
                    cardPlatformView = view
                    Log.d("MainActivity", "Card view instance captured")
                }
        )

        registry.registerViewFactory(
                "flow_googlepay_view",
                GooglePayViewFactory(messenger, this) { view ->
                    googlePayPlatformView = view
                    Log.d("MainActivity", "Google Pay view instance captured")
                }
        )

        // Set up method channel for calls from Flutter
        MethodChannel(messenger, CHANNEL).setMethodCallHandler { call, result ->
            handleMethodCall(call, result)
        }
    }

    /** Handle method calls from Flutter */
    private fun handleMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.d("MainActivity", "Method call: ${call.method}")

        when (call.method) {
            // ==================== CARD METHODS ====================
            "initCardView" -> {
                // Card view initialization happens in PlatformView
                // This method can be used for additional setup if needed
                result.success(true)
            }
            "validateCard" -> {
                if (cardPlatformView == null) {
                    result.error("CARD_NOT_READY", "Card view not initialized", null)
                    return
                }
                val isValid = cardPlatformView?.validateCard() ?: false
                result.success(isValid)
            }
            "tokenizeCard" -> {
                if (cardPlatformView == null) {
                    result.error("CARD_NOT_READY", "Card view not initialized", null)
                    return
                }
                cardPlatformView?.tokenizeCard(result)
            }
            "getSessionData" -> {
                if (cardPlatformView == null) {
                    result.error("CARD_NOT_READY", "Card view not initialized", null)
                    return
                }
                cardPlatformView?.getSessionData(result)
            }

            // ==================== GOOGLE PAY METHODS ====================
            "initGooglePay" -> {
                // Google Pay initialization happens in PlatformView
                result.success(true)
            }
            "checkGooglePayAvailability" -> {
                if (googlePayPlatformView == null) {
                    result.error("GOOGLEPAY_NOT_READY", "Google Pay view not initialized", null)
                    return
                }
                val isAvailable = googlePayPlatformView?.checkAvailability() ?: false
                result.success(isAvailable)
            }
            "launchGooglePaySheet" -> {
                if (googlePayPlatformView == null) {
                    result.error("GOOGLEPAY_NOT_READY", "Google Pay view not initialized", null)
                    return
                }
                @Suppress("UNCHECKED_CAST") val requestData = call.arguments as? Map<String, Any>
                googlePayPlatformView?.launchPaymentSheet(requestData, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}
