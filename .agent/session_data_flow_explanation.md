# Session Data Flow - Get Session Data Without Completing Payment

## Overview
This implementation allows you to retrieve session data from the Checkout.com SDK without completing the payment. The session data can then be sent to your backend for processing.

## How It Works

### Flow Diagram
```
User fills card details
         ↓
Flutter calls: _paymentBridge.submit()
         ↓
Android MainActivity: getSessionData() method
         ↓
CardPlatformView: cardComponent.submit()
         ↓
SDK triggers: handleSubmit callback with sessionData (String)
         ↓
sendSessionData() sends to Flutter via "sessionDataReady" event
         ↓
handleSubmit returns: ApiCallResult.Failure (prevents payment completion)
         ↓
Flutter receives session data string  
         ↓
Backend submission (your implementation)
```

## Key Implementation Details

### 1. Android - CardPlatformView.kt

#### handleSubmit Callback (Lines 102-115)
```kotlin
handleSubmit = { sessionData ->
    Log.d("CardPlatformView", "handleSubmit called with sessionData: $sessionData")
    
    // Send session data to Flutter for backend submission
    sendSessionData(sessionData)
    
    // Return Failure to prevent SDK from completing payment
    // This allows us to get session data without auto-completing payment
    ApiCallResult.Failure
},
```

**Critical Point:** 
- Returns `ApiCallResult.Failure` to **prevent the payment from being auto-completed**
- This is NOT an error - it's intentional to stop the SDK from proceeding to payment
- The session data is still captured and sent to Flutter before returning Failure

#### getSessionData Method (Lines 321-354)
```kotlin
fun getSessionData(result: MethodChannel.Result) {
    if (!isInitialized || !::cardComponent.isInitialized) {
        result.error("CARD_NOT_READY", "Card component not initialized", null)
        return
    }

    scope.launch {
        try {
            Log.d("CardPlatformView", "Starting session data submission...")
            
            // Submit session data on background thread
            withContext(Dispatchers.Default) { cardComponent.submit() }
            
            // Result will be sent via handleSubmit callback
            withContext(Dispatchers.Main) {
                result.success(mapOf("status" to "processing"))
            }
        } catch (e: Exception) {
            Log.e("CardPlatformView", "Session data submission error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                result.error(
                    "SESSION_DATA_ERROR",
                    e.message ?: "Session data submission failed",
                    null
                )
            }
        }
    }
}
```

**What it does:**
- Calls `cardComponent.submit()` which triggers the SDK's submit flow
- The SDK then calls `handleSubmit` callback with the session data
- Returns immediately with "processing" status
- Actual session data comes through the `handleSubmit` callback

#### sendSessionData Method (Lines 394-417)
```kotlin
private fun sendSessionData(sessionData: String) {
    runOnMainThread {
        try {
            val data = mapOf("sessionData" to sessionData)
            
            channel.invokeMethod("sessionDataReady", data)
            Log.d("CardPlatformView", "Session data sent to Flutter for backend submission")
        } catch (e: Exception) {
            Log.e("CardPlatformView", "Failed to send session data: ${e.message}", e)
            sendError("SESSION_DATA_ERROR", e.message ?: "Failed to send session data")
        }
    }
}
```

**What it does:**
- Wraps the session data string in a Map
- Sends it to Flutter via "sessionDataReady" method channel event
- Handles errors gracefully

### 2. Android - MainActivity.kt (Lines 76-82)

```kotlin
"getSessionData" -> {
    if (cardPlatformView == null) {
        result.error("CARD_NOT_READY", "Card view not initialized", null)
        return
    }
    cardPlatformView?.getSessionData(result)
}
```

Routes the Flutter call to the CardPlatformView instance.

### 3. Flutter - payment_bridge.dart

#### Method Call Handler (Lines 53-58)
```dart
case 'sessionDataReady':
  final args = call.arguments as Map<String, dynamic>;
  final result = args['sessionData'] as String;
  log('[Checkout]: ✅ Session data: $result');
  onSessionData?.call(result);
  break;
```

**Critical:** 
- Arguments come as a Map with key 'sessionData'
- Extract the String value from the map
- Call the registered callback with the session data

#### submit() Method (Lines 107-121)
```dart
Future<void> submit() async {
  try {
    log('[Checkout]: 🔄 Submitting...');
    await _channel.invokeMethod('getSessionData');
    // Result will come via onSessionData callback
  } on PlatformException catch (e) {
    log('[Checkout]: ❌ Submit failed: ${e.message}');
    onPaymentError?.call(
      PaymentErrorResult(
        errorCode: e.code,
        errorMessage: e.message ?? 'Submit failed',
      ),
    );
  }
}
```

Triggers the session data retrieval flow.

### 4. Flutter - main.dart Integration

#### Setup (Line 70)
```dart
_paymentBridge.onSessionData = _handleSessionData;
```

#### Handler (Lines 85-93)
```dart
void _handleSessionData(String sessionData) {
  setState(() => _isProcessing = false);
  _showResultDialog(
    'Session Data',
    'Session Data: $sessionData',
    isSuccess: true,
  );
}
```

#### Trigger (Lines 190-193)
```dart
Future<void> _getSessionData() async {
  setState(() => _isProcessing = true);
  await _paymentBridge.submit();
}
```

## Important Notes

### Why ApiCallResult.Failure?

The `handleSubmit` callback returns `ApiCallResult.Failure` to:
1. **Prevent automatic payment completion** by the SDK
2. **Stop the payment flow** after capturing session data
3. **Allow manual backend processing** with the session data

This is **intentional and correct** - it's not an error condition.

### Session Data Format

The `sessionData` is a **String** that contains:
- Encoded payment session information
- Required data for backend payment processing
- Should be sent as-is to your backend

Your backend then uses this string to complete the payment via Checkout.com's API.

### Error Handling

Expected behavior:
- The SDK may log a "failure" after `handleSubmit` returns `Failure` - this is expected
- You will NOT get `onSuccess` or `onPaymentSuccess` callbacks (intentional)
- You WILL get the session data via `onSessionData` callback
- Any actual errors will be sent via `onPaymentError` callback

## Testing the Flow

1. Run the Flutter app
2. Fill in card details in the card component
3. Press the "Submit" button
4. Check Android logs:
   - `"Starting session data submission..."`
   - `"handleSubmit called with sessionData: [session_data_string]"`
   - `"Session data sent to Flutter for backend submission"`
5. Check Flutter logs:
   - `"✅ Session data: [session_data_string]"`
6. Verify the session data appears in your dialog/UI
7. **Important:** The payment is NOT completed at this point
8. Send the session data to your backend to complete the payment

## Next Steps: Backend Integration

In your production code, replace `_handleSessionData` with actual backend submission:

```dart
void _handleSessionData(String sessionData) async {
  setState(() => _isProcessing = true);
  
  try {
    // Send to your backend
    final response = await http.post(
      Uri.parse('YOUR_BACKEND_URL/process-payment'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'session_data': sessionData}),
    );
    
    if (response.statusCode == 200) {
      // Payment processed successfully
      _showResultDialog('Success', 'Payment completed!', isSuccess: true);
    } else {
      // Payment failed
      _showResultDialog('Error', 'Payment failed', isSuccess: false);
    }
  } catch (e) {
    _showResultDialog('Error', 'Network error: $e', isSuccess: false);
  } finally {
    setState(() => _isProcessing = false);
  }
}
```

## Summary

✅ Session data is captured WITHOUT completing payment  
✅ Payment completion is controlled by your backend  
✅ Full control over payment flow  
✅ Proper error handling throughout the chain  
✅ Clean separation between UI and payment logic
