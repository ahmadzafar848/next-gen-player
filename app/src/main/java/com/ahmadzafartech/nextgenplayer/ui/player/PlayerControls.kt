package com.ahmadzafartech.nextgenplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.ahmadzafartech.nextgenplayer.util.formatMillis

@Composable
fun PlayerControls(
    currentMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x88000000))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMillis(currentMs), color = Color.White, modifier = Modifier.width(56.dp))
            Slider(
                value = currentMs.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..(totalMs.coerceAtLeast(1L).toFloat()),
                modifier = Modifier.weight(1f)
            )
            Text(formatMillis(totalMs), color = Color.White, modifier = Modifier.width(56.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White) }
            IconButton(onClick = onPlayPause) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White) }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White) }
        }
    }
}
