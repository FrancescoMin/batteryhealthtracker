package com.fivestars.batterytracker

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private const val TAG = "HighRefreshRate"

/**
 * Enforces the maximum supported refresh rate (e.g. 120 Hz) on an Android [Window].
 * Ensures that both LTPS fixed-frequency and LTPO variable-frequency displays
 * lock to the highest available display mode and avoid downclocking to 90 Hz or 60 Hz.
 */
fun enforceHighRefreshRate(window: Window, context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = try {
                window.context.display
            } catch (_: Throwable) {
                null
            } ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)

            val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                val params = window.attributes
                var changed = false
                if (params.preferredDisplayModeId != maxMode.modeId) {
                    params.preferredDisplayModeId = maxMode.modeId
                    changed = true
                }
                if (params.preferredRefreshRate != maxMode.refreshRate) {
                    params.preferredRefreshRate = maxMode.refreshRate
                    changed = true
                }
                if (changed) {
                    window.attributes = params
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val params = window.attributes
            if (params.preferredRefreshRate != 120f) {
                params.preferredRefreshRate = 120f
                window.attributes = params
            }
        }
    } catch (e: Throwable) {
        Log.d(TAG, "Could not enforce high refresh rate: ${e.message}")
    }
}

/**
 * Composable side-effect that discovers the underlying [Window] of any Compose Dialog / AlertDialog
 * through [DialogWindowProvider] and locks its refresh rate to 120 Hz.
 */
@Composable
fun EnforceDialogHighRefreshRate() {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(view) {
        val applyRate = {
            var current: ViewParent? = view.parent
            var dialogWindow: Window? = null
            while (current != null) {
                if (current is DialogWindowProvider) {
                    dialogWindow = current.window
                    break
                }
                current = current.parent
            }
            if (dialogWindow != null) {
                enforceHighRefreshRate(dialogWindow, context)
            }
        }

        applyRate()

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                applyRate()
            }
            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(listener)

        onDispose {
            view.removeOnAttachStateChangeListener(listener)
        }
    }
}

/**
 * Drop-in replacement for [AlertDialog] that automatically enforces 120 Hz on the dialog window.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            EnforceDialogHighRefreshRate()
            confirmButton()
        },
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = {
            EnforceDialogHighRefreshRate()
            title?.invoke()
        },
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
