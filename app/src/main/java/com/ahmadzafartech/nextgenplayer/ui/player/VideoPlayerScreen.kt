package com.ahmadzafartech.nextgenplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay

enum class GestureMode { NONE, SEEK, VOLUME, BRIGHTNESS }

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: VideoPlayerViewModel,
    navController: NavController,
    videoUri: Uri
) {
    val context = LocalContext.current
    val activity = context as Activity
    val exoPlayer = playerViewModel.exoPlayer ?: return
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // --- STATE MANAGEMENT ---
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var playbackPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Scrubbing & Gestures
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }
    var activeGesture by remember { mutableStateOf(GestureMode.NONE) }

    // Volume/Brightness
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volumeLevel by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1))
    }
    var brightnessLevel by remember {
        mutableStateOf(activity.window.attributes.screenBrightness.let { if (it < 0) 0.5f else it })
    }

    // --- LIFECYCLE & PROGRESS UPDATES ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                isPlaying = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            exoPlayer.pause()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val orientationManager = VideoOrientationManager(context, activity)
        orientationManager.enable()

        onDispose {
            orientationManager.disable()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // --- UPDATE PLAYBACK POSITION & DURATION SMOOTHLY ---
    LaunchedEffect(exoPlayer, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                val dur = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                duration = dur
                playbackPosition = exoPlayer.currentPosition.coerceIn(0, dur)
            }
            delay(100L) // smooth updates ~10 times/sec
        }
    }

    // --- AUTO HIDE CONTROLS ---
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- VIDEO SURFACE + GESTURES ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            val isForward = offset.x > size.width / 2
                            val safeDuration = duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + if (isForward) 10000 else -10000).coerceIn(0, safeDuration)
                            exoPlayer.seekTo(newPos)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeGesture = if (offset.x < size.width / 2) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                        },
                        onDragEnd = { activeGesture = GestureMode.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.005f
                            if (activeGesture == GestureMode.BRIGHTNESS) {
                                brightnessLevel = (brightnessLevel - dragAmount.y * sensitivity).coerceIn(0f, 1f)
                                activity.window.attributes = activity.window.attributes.apply { screenBrightness = brightnessLevel }
                            } else if (activeGesture == GestureMode.VOLUME) {
                                volumeLevel = (volumeLevel - dragAmount.y * sensitivity).coerceIn(0f, 1f)
                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * max).toInt(), 0)
                            }
                        }
                    )
                }
        )

        // --- VOLUME/BRIGHTNESS OVERLAYS ---
        if (activeGesture == GestureMode.BRIGHTNESS) {
            NetflixVerticalSlider(icon = Icons.Default.WbSunny, value = brightnessLevel, Modifier.align(Alignment.CenterStart))
        }
        if (activeGesture == GestureMode.VOLUME) {
            NetflixVerticalSlider(icon = Icons.AutoMirrored.Filled.VolumeUp, value = volumeLevel, Modifier.align(Alignment.CenterEnd))
        }

        // --- MAIN CONTROLS ---
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {

                IconButton(
                    onClick = { exoPlayer.pause(); navController.popBackStack() },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // --- CENTER ICONS ---
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }) {
                        Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                    Spacer(Modifier.width(40.dp))
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isPlaying = exoPlayer.isPlaying
                        },
                        modifier = Modifier.size(90.dp)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(40.dp))
                    IconButton(onClick = {
                        val safeDuration = duration.coerceAtLeast(0L)
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceIn(0, safeDuration))
                    }) {
                        Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                }

                // --- BOTTOM SEEK BAR ---
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 20.dp, end = 20.dp)) {
                    NetflixSeekBar(
                        position = if (isScrubbing) scrubPosition else playbackPosition,
                        duration = duration,
                        onScrubStart = {
                            isScrubbing = true
                            scrubPosition = playbackPosition
                        },
                        onScrubbing = { newTime ->
                            scrubPosition = newTime
                        },
                        onScrubEnd = { finalTime ->
                            exoPlayer.seekTo(finalTime)
                            playbackPosition = finalTime
                            isScrubbing = false
                        }
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), Arrangement.SpaceBetween) {
                        val current = if (isScrubbing) scrubPosition else playbackPosition
                        Text(formatTime(current), color = Color.White, fontSize = 12.sp)
                        Text("-${formatTime((duration - current).coerceAtLeast(0))}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NetflixSeekBar(
    position: Long,
    duration: Long,
    onScrubStart: () -> Unit,
    onScrubbing: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubbedPosition by remember { mutableStateOf(position) }
    val progress = if (duration > 0) scrubbedPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = {
                        scrubbedPosition = position
                        onScrubStart()
                    },
                    onDragEnd = {
                        onScrubEnd(scrubbedPosition)
                    },
                    onDragCancel = {
                        onScrubEnd(scrubbedPosition)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newTime = (newProgress * duration).toLong()
                        scrubbedPosition = newTime
                        onScrubbing(newTime)
                    }
                )
            }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val tappedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    val tappedTime = (tappedProgress * duration).toLong()
                    scrubbedPosition = tappedTime
                    onScrubEnd(tappedTime)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
        ) {
            val width = size.width
            val centerY = size.height / 2

            drawLine(
                color = Color.White.copy(0.3f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE50914),
                start = Offset(0f, centerY),
                end = Offset(width * progress, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFE50914),
                radius = 7.dp.toPx(),
                center = Offset(width * progress, centerY)
            )
        }
    }
}

@Composable
fun NetflixVerticalSlider(icon: ImageVector, value: Float, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp).width(40.dp).height(200.dp)
            .background(Color.Black.copy(0.7f), RoundedCornerShape(20.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp).padding(bottom = 8.dp))
        Box(modifier = Modifier.width(4.dp).height(140.dp).background(Color.Gray, CircleShape)) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(value).align(Alignment.BottomCenter).background(Color.White, CircleShape))
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
