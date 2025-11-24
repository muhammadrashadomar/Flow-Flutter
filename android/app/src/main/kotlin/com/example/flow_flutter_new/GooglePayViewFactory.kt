package com.example.flow_flutter_new.views

import android.content.Context
import androidx.activity.ComponentActivity
import com.example.flow_flutter_new.GooglePayPlatformView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

/**
 * Factory for creating GooglePayPlatformView instances Provides callback to capture view instance
 * for method channel operations
 */
class GooglePayViewFactory(
        private val messenger: BinaryMessenger,
        private val activity: ComponentActivity,
        private val onViewCreated: ((GooglePayPlatformView) -> Unit)? = null
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val view = GooglePayPlatformView(activity, args, messenger)
        onViewCreated?.invoke(view)
        return view
    }
}
