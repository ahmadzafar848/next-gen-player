package com.ahmadzafartech.nextgenplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ───────── DataStore extension ─────────
val Context.dataStore by preferencesDataStore("video_positions")

enum class GestureMode { NONE, SEEK, VOLUME, BRIGHTNESS, SPEED }

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: VideoPlayerViewModel,
    navController: NavController,
    videoUri: Uri // unique video identifier
) {
    val context = LocalContext.current
    val activity = context as Activity
    val exoPlayer = playerViewModel.exoPlayer!!

    var overlayText by remember { mutableStateOf<String?>(null) }
    var overlayVisible by remember { mutableStateOf(false) }

    var isOrientationLocked by remember { mutableStateOf(false) }
    var currentOrientation by remember { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    val key = longPreferencesKey(videoUri.toString())

    val coroutineScope = rememberCoroutineScope()

    fun showOverlay(text: String) {
        overlayText = text
        overlayVisible = true
    }

    // ───────── Load last playback position automatically ─────────
    LaunchedEffect(videoUri) {
        val saved = context.dataStore.data.first()[key] ?: 0L
        if (saved > 2000L) { // only if >2s
            exoPlayer.seekTo(saved)
        }
    }

    // ───────── Orientation Listener ─────────
    DisposableEffect(isOrientationLocked) {
        val orientationListener = object : OrientationEventListener(
            context,
            SensorManager.SENSOR_DELAY_UI
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (isOrientationLocked || orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return
                currentOrientation = when {
                    orientation in 80..100 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    orientation in 260..280 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    orientation in 350..360 || orientation in 0..10 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else -> currentOrientation
                }
                activity.requestedOrientation = currentOrientation
            }
        }
        orientationListener.enable()
        onDispose {
            orientationListener.disable()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ───────── Lifecycle Pause/Resume ─────────
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = playerViewModel.isPlaying
                else -> Unit
            }
        }
        (context as LifecycleOwner).lifecycle.addObserver(observer)
        onDispose { (context as LifecycleOwner).lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ───────── Video Player View with Gestures ─────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->

                val playerView = PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    subtitleView?.visibility = View.VISIBLE
                }

                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                var gestureMode = GestureMode.NONE
                var lastStepY = 0f
                var speed = exoPlayer.playbackParameters.speed

                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {

                    override fun onDown(e: MotionEvent): Boolean {
                        gestureMode = GestureMode.NONE
                        lastStepY = e.y
                        speed = exoPlayer.playbackParameters.speed
                        return true
                    }

                    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                        if (e1 == null) return false // only e1 can be null
                        val width = playerView.width
                        val deltaX = e2.x - e1.x
                        val deltaY = e2.y - e1.y

                        if (gestureMode == GestureMode.NONE) {
                            gestureMode = when {
                                kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) -> GestureMode.SEEK
                                e1.x < width * 0.33f -> GestureMode.BRIGHTNESS
                                e1.x > width * 0.66f -> GestureMode.VOLUME
                                else -> GestureMode.SPEED
                            }
                        }

                        val step = lastStepY - e2.y
                        if (kotlin.math.abs(step) < 8) return true
                        lastStepY = e2.y

                        when (gestureMode) {
                            GestureMode.SEEK -> {
                                val seekMs = (deltaX / width * 90_000).toLong()
                                exoPlayer.seekTo((exoPlayer.currentPosition + seekMs).coerceIn(0, exoPlayer.duration))
                                showOverlay("⏩ ${seekMs / 1000}s")
                            }
                            GestureMode.BRIGHTNESS -> {
                                val attrs = activity.window.attributes
                                attrs.screenBrightness = (attrs.screenBrightness + if (step > 0) 0.02f else -0.02f).coerceIn(0.1f, 1f)
                                activity.window.attributes = attrs
                                showOverlay("☀ ${(attrs.screenBrightness * 100).toInt()}%")
                            }
                            GestureMode.VOLUME -> {
                                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val newVol = (cur + if (step > 0) 1 else -1).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                showOverlay("🔊 ${(newVol * 100) / maxVolume}%")
                            }
                            GestureMode.SPEED -> {
                                speed = (speed + if (step > 0) 0.05f else -0.05f).coerceIn(0.5f, 2.0f)
                                exoPlayer.setPlaybackSpeed(speed)
                                showOverlay("⏱ ${"%.2f".format(speed)}x")
                            }
                            else -> {}
                        }
                        return true
                    }
                })

                playerView.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    false
                }

                playerView
            }
        )

        // ───────── Overlay ─────────
        if (overlayVisible && overlayText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    overlayText!!,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            LaunchedEffect(overlayText) {
                delay(700)
                overlayVisible = false
            }
        }

        // ───────── Orientation Lock Button ─────────
        IconButton(
            onClick = {
                isOrientationLocked = !isOrientationLocked
                showOverlay(if (isOrientationLocked) "🔒 Orientation Locked" else "🔓 Orientation Unlocked")
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = if (isOrientationLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = Color.White
            )
        }

        // ───────── PIP Button ─────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IconButton(
                onClick = {
                    activity.enterPictureInPictureMode(
                        android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.PictureInPicture, contentDescription = "PIP", tint = Color.White)
            }
        }

        // ───────── BackHandler ─────────
        BackHandler {
            exoPlayer.pause()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            navController.popBackStack()
        }
    }

    // ───────── Periodic Save Playback Position ─────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val pos = exoPlayer.currentPosition
            coroutineScope.launch {
                context.dataStore.edit { it[key] = pos }
            }
        }
    }
}

// ───────── Format Time Helper ─────────
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
