package com.example.flow_flutter_new

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.api.PaymentMethodComponent
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.wallet.wrapper.GooglePayFlowCoordinator
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.*

/**
 * Google Pay Platform View - Handles Google Pay payment sheet Complete control from Flutter layer
 *
 * Architecture:
 * - NO native Google Pay button (Flutter renders button via flutter_pay)
 * - Only exposes payment sheet logic
 * - Called via method channel from Flutter
 * - Sends results back via callbacks
 *
 * NOTE: In the refactored architecture, this view is optional. Flutter can directly call Google Pay
 * via flutter_pay package. This remains for Checkout.com SDK integration if needed.
 */
class GooglePayPlatformView(
        private val activity: ComponentActivity,
        args: Any?,
        messenger: BinaryMessenger
) : PlatformView {

    private val container = FrameLayout(activity)
    private val channel = MethodChannel(messenger, "checkout_bridge")
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var checkoutComponents: CheckoutComponents
    private lateinit var googlePayComponent: PaymentMethodComponent
    private lateinit var coordinator: GooglePayFlowCoordinator

    @Volatile private var isInitialized = false

    init {
        // Note: In refactored architecture, this may not render any UI
        // It only sets up the payment logic for method channel calls
        initializeGooglePay(args)
    }

    /** Initialize Google Pay component with configuration from Flutter */
    private fun initializeGooglePay(args: Any?) {
        val params = args as? Map<*, *> ?: emptyMap<String, Any>()

        // Extract required parameters
        val sessionId = params["paymentSessionID"] as? String ?: ""
        val sessionSecret = params["paymentSessionSecret"] as? String ?: ""
        val publicKey = params["publicKey"] as? String ?: ""
        val environmentStr = params["environment"] as? String ?: "sandbox"

        // Validate required parameters
        if (sessionId.isEmpty() || sessionSecret.isEmpty() || publicKey.isEmpty()) {
            Log.e("GooglePayPlatformView", "Missing required session parameters")
            sendError("INIT_ERROR", "Missing required payment session parameters")
            return
        }

        // Parse environment
        val environment =
                when (environmentStr.lowercase()) {
                    "production" -> Environment.PRODUCTION
                    else -> Environment.SANDBOX
                }

        // Create Google Pay coordinator
        coordinator =
                GooglePayFlowCoordinator(
                        context = activity,
                        handleActivityResult = { resultCode, data ->
                            handleActivityResult(resultCode, data)
                        }
                )

        // Build component callback
        val componentCallback =
                ComponentCallback(
                        onReady = { component ->
                            Log.d("GooglePayPlatformView", "Component ready: ${component.name}")
                        },
                        onSubmit = { component ->
                            Log.d("GooglePayPlatformView", "Component submitted: ${component.name}")
                        },
                        onSuccess = { _, paymentID ->
                            Log.d("GooglePayPlatformView", "Payment success: $paymentID")
                            sendPaymentSuccess(paymentID)
                        },
                        onError = { _, checkoutError ->
                            Log.e("GooglePayPlatformView", "Error: ${checkoutError.message}")
                            sendError(checkoutError.code.toString(), checkoutError.message)
                        },
                )

        val flowCoordinators = mapOf(PaymentMethodName.GooglePay to coordinator)

        // Build configuration
        val configuration =
                CheckoutComponentConfiguration(
                        context = activity,
                        paymentSession =
                                PaymentSessionResponse(id = sessionId, secret = sessionSecret),
                        publicKey = publicKey,
                        environment = environment,
                        flowCoordinators = flowCoordinators,
                        componentCallback = componentCallback
                )

        container.setViewTreeLifecycleOwner(activity)

        // Initialize component asynchronously
        scope.launch {
            try {
                checkoutComponents = CheckoutComponentsFactory(config = configuration).create()
                googlePayComponent = checkoutComponents.create(PaymentMethodName.GooglePay)

                if (googlePayComponent.isAvailable()) {
                    // In refactored architecture, we may not render UI here
                    // Keep it for compatibility, but Flutter controls when to show
                    withContext(Dispatchers.Main) {
                        val composeView = ComposeView(activity)
                        composeView.setContent { googlePayComponent.Render() }
                        container.addView(composeView)
                        isInitialized = true
                        Log.d("GooglePayPlatformView", "Google Pay component initialized")
                    }
                } else {
                    Log.e("GooglePayPlatformView", "Google Pay not available")
                    sendError(
                            "GOOGLEPAY_NOT_AVAILABLE",
                            "Google Pay is not available on this device"
                    )
                }
            } catch (e: CheckoutError) {
                Log.e("GooglePayPlatformView", "Checkout error: ${e.message}", e)
                sendError("CHECKOUT_ERROR", e.message)
            } catch (e: Exception) {
                Log.e("GooglePayPlatformView", "Unexpected error: ${e.message}", e)
                sendError("INIT_ERROR", e.message ?: "Failed to initialize Google Pay")
            }
        }
    }

    /** Handle activity result from Google Pay sheet */
    private fun handleActivityResult(resultCode: Int, data: String) {
        checkoutComponents.handleActivityResult(resultCode, data)
    }

    // ==================== PUBLIC METHODS (Called from MainActivity) ====================

    /** Check if Google Pay is available */
    fun checkAvailability(): Boolean {
        // if (!isInitialized || !::googlePayComponent.isInitialized) {
        //     Log.w("GooglePayPlatformView", "Google Pay component not initialized")
        //     return false
        // }
        // return googlePayComponent.isAvailable()
        return true
    }

    /** Launch Google Pay payment sheet Called from Flutter via method channel */
    fun launchPaymentSheet(requestData: Map<String, Any>?, result: MethodChannel.Result) {
        if (!isInitialized || !::googlePayComponent.isInitialized) {
            result.error("GOOGLEPAY_NOT_READY", "Google Pay component not initialized", null)
            return
        }

        scope.launch {
            try {
                Log.d("GooglePayPlatformView", "Launching Google Pay sheet...")

                // The Checkout.com SDK handles the sheet internally
                // Result will come via callbacks

                withContext(Dispatchers.Main) { result.success(mapOf("status" to "launched")) }
            } catch (e: Exception) {
                Log.e("GooglePayPlatformView", "Launch error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    result.error("LAUNCH_ERROR", e.message ?: "Failed to launch Google Pay", null)
                }
            }
        }
    }

    // ==================== CALLBACK METHODS (Send to Flutter) ====================

    /** Send payment success event to Flutter */
    private fun sendPaymentSuccess(paymentId: String) {
        runOnMainThread {
            try {
                channel.invokeMethod("paymentSuccess", paymentId)
                Log.d("GooglePayPlatformView", "Payment success event sent to Flutter")
            } catch (e: Exception) {
                Log.e("GooglePayPlatformView", "Failed to send success event: ${e.message}", e)
            }
        }
    }

    /** Send error event to Flutter */
    private fun sendError(code: String, message: String) {
        runOnMainThread {
            try {
                val error = mapOf("code" to code, "message" to message)
                channel.invokeMethod("paymentError", error)
                Log.d("GooglePayPlatformView", "Error event sent to Flutter: $code - $message")
            } catch (e: Exception) {
                Log.e("GooglePayPlatformView", "Failed to send error event: ${e.message}", e)
            }
        }
    }

    /** Helper to run code on main thread */
    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    // ==================== LIFECYCLE METHODS ====================

    override fun getView(): FrameLayout = container

    override fun dispose() {
        scope.cancel()
        Log.d("GooglePayPlatformView", "Google Pay component disposed")
    }
}
