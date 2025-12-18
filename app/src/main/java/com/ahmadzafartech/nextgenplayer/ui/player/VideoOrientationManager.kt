package com.ahmadzafartech.nextgenplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener

class VideoOrientationManager(
    context: Context,
    private val activity: Activity,
) : OrientationEventListener(context) {

    private var lastOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    override fun onOrientationChanged(orientation: Int) {
        if (orientation == ORIENTATION_UNKNOWN) return

        val newOrientation =
            if (orientation in 60..120 || orientation in 240..300)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (newOrientation != lastOrientation) {
            activity.requestedOrientation = newOrientation
            lastOrientation = newOrientation
        }
    }
}

