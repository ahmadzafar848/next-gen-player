package com.ahmadzafartech.nextgenplayer.ui.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ahmadzafartech.nextgenplayer.domain.Video
import com.ahmadzafartech.nextgenplayer.util.formatMillis
import com.ahmadzafartech.nextgenplayer.util.gradientFromString
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoListViewModel
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoListScreen(
    folderPath: String,
    navController: NavController,
    listViewModel: VideoListViewModel,
    playerViewModel: VideoPlayerViewModel
) {
    val context = LocalContext.current
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(folderPath) { listViewModel.loadVideos(context, folderPath) }

    val filteredVideos = if (searchQuery.isEmpty()) listViewModel.videos
    else listViewModel.videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    val folderGradient = remember(folderPath) { gradientFromString(folderPath) }
    val topBarHeight = 56.dp // standard TopAppBar height


    Scaffold(
        topBar = {
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .background(folderGradient)
            ){
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                placeholder = { Text("Search videos", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else MarqueeText(folderPath, modifier = Modifier.fillMaxWidth())
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = Color.Transparent // make screen transparent to show background Box
    ) { padding ->
        // Full screen gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    folderGradient
                )
                .padding(padding)
        ) {
            if (filteredVideos.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("No videos in this folder", color = Color.White) }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVideos.size) { index ->
                        val video = filteredVideos[index]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        playerViewModel.setPlaylist(
                                            context,
                                            filteredVideos.toList(),
                                            index
                                        )
                                        navController.navigate("player/${Uri.encode(video.uri.toString())}")
                                    },
                                    onLongClick = {
                                        selectedVideo = video
                                        scope.launch { bottomSheetState.show() }
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(12.dp), // more prominent elevation
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF1F1F1F),
                                                    Color(0xFF2A2A2A)
                                                )
                                            )
                                        )
                                ) {
                                    val bitmap = video.thumbnail
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = video.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = video.uri,
                                            contentDescription = video.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Cinematic bottom gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.55f)
                                                    ),
                                                    startY = 300f
                                                )
                                            )
                                    )

                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .align(Alignment.Center)
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = video.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                val durationText = video.duration?.let { formatMillis(it) } ?: "Unknown"
                                Text(
                                    text = durationText,
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (selectedVideo != null) {
            ModalBottomSheet(
                onDismissRequest = { },
                sheetState = bottomSheetState,
                dragHandle = { Spacer(Modifier.height(8.dp)) },
                containerColor = Color.Transparent, // make sheet transparent
                tonalElevation = 0.dp
            ) {
                val video = selectedVideo!!
                // Gradient Box for Bottom Sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            folderGradient,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text("Duration: ${video.duration?.let { formatMillis(it) } ?: "Unknown"}", color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        video.thumbnail?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = video.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                            folderGradient
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.3f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    .clickable { shareVideo(context, video) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Share",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareVideo(context: Context, video: Video) {
    try {
        val file = File(video.uri.path!!)
        val contentUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 24.sp, color = Color.White)
) {
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.CenterStart // Start for scrolling, will adjust below
    ) {
        var textWidth by remember { mutableStateOf(0) }
        var boxWidth by remember { mutableStateOf(0) }

        val shouldScroll = textWidth > boxWidth

        val offsetX by if (shouldScroll) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -textWidth.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            // If no scrolling, center the text
            remember { mutableStateOf(0f) }
        }

        BasicText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = style,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    textWidth = coordinates.size.width
                    if (boxWidth == 0) {
                        boxWidth = coordinates.parentLayoutCoordinates?.size?.width ?: 0
                    }
                }
                .offset {
                    // If not scrolling, center it
                    val x = if (shouldScroll) offsetX.toInt() else (boxWidth - textWidth) / 2
                    IntOffset(x, 0)
                }
        )
    }
}




