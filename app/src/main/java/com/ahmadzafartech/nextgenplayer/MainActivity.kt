package com.ahmadzafartech.nextgenplayer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ahmadzafartech.nextgenplayer.ui.folder.FolderListScreen
import com.ahmadzafartech.nextgenplayer.ui.player.VideoPlayerScreen
import com.ahmadzafartech.nextgenplayer.ui.video.VideoListScreen
import com.ahmadzafartech.nextgenplayer.viewmodel.FolderViewModel
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoListViewModel
import com.ahmadzafartech.nextgenplayer.viewmodel.VideoPlayerViewModel
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Parabolic
import com.exyte.animatednavbar.animation.indendshape.Height
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(this)
        }
    }
}

/* ----------------------- MAIN SCREEN ----------------------- */

@Composable
fun MainScreen(activity: ComponentActivity) {

    val systemUiController = rememberSystemUiController()
    val darkColor = Color(0xFF121212)

    SideEffect {
        systemUiController.setStatusBarColor(darkColor, false)
        systemUiController.setNavigationBarColor(darkColor, false)
    }

    var selectedTab by remember { mutableStateOf(BottomTab.FOLDERS) }
    val folderNavController = rememberNavController()

    val folderVm: FolderViewModel = viewModel()
    val listVm: VideoListViewModel = viewModel()
    val playerVm: VideoPlayerViewModel = viewModel()

    /* ---------- Permission ---------- */

    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var shouldShowRationale by remember { mutableStateOf(false) }

    fun checkPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        permissionGranted =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        shouldShowRationale = !permissionGranted && ActivityCompat.shouldShowRequestPermissionRationale(
            context as Activity, permission
        )
    }

    // Initial check
    LaunchedEffect(Unit) { checkPermission() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        checkPermission()
    }

    // Re-check permission when app resumes
    DisposableEffect(Unit) {
        val lifecycle = (context as ComponentActivity).lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    /* ---------- Route Observer ---------- */

    val backStackEntry by folderNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar =
        selectedTab != BottomTab.FOLDERS || currentRoute == "folders"

    /* ---------- UI ---------- */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkColor)
    ) {

        if (!permissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (shouldShowRationale)
                            "Permission is required to access videos"
                        else
                            "Permission denied permanently. Enable in settings",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_VIDEO
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE

                        if (shouldShowRationale) {
                            permissionLauncher.launch(permission)
                        } else {
                            // Open app settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            when (selectedTab) {
                BottomTab.FOLDERS -> {
                    FolderNavGraph(
                        navController = folderNavController,
                        folderVm = folderVm,
                        listVm = listVm,
                        playerVm = playerVm
                    )
                }
                BottomTab.SECRET -> SecretFolderScreen()
                BottomTab.PROFILE -> ProfileScreen()
            }
        }

        if (showBottomBar) {
            ExyteBottomBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    if (it == BottomTab.FOLDERS) {
                        folderNavController.popBackStack("folders", inclusive = false)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/* ----------------------- NAV GRAPH ----------------------- */

@Composable
fun FolderNavGraph(
    navController: NavHostController,
    folderVm: FolderViewModel,
    listVm: VideoListViewModel,
    playerVm: VideoPlayerViewModel
) {
    NavHost(navController, startDestination = "folders") {

        composable("folders") {
            FolderListScreen(
                navController = navController,
                viewModel = folderVm
            )
        }

        composable("video_list/{folder}") { backStack ->
            val encoded = backStack.arguments?.getString("folder") ?: ""
            val folder = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
            VideoListScreen(folder, navController, listVm, playerVm)
        }

        composable(
            "player/{videoUri}",
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStack ->
            val videoUri = backStack.arguments?.getString("videoUri")?.toUri()
            videoUri?.let {
                VideoPlayerScreen(playerVm, navController, it)
            }
        }
    }
}

/* ----------------------- BOTTOM BAR ----------------------- */

enum class BottomTab(val title: String, val icon: ImageVector) {
    FOLDERS("Folders", Icons.Default.Folder),
    SECRET("Secret", Icons.Default.Lock),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun ExyteBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = BottomTab.values().toList()
    val selectedIndex = tabs.indexOf(selectedTab)

    AnimatedNavigationBar(
        selectedIndex = selectedIndex,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        barColor = Color.Transparent,
        ballColor = Color(0xFF3B82F6),
        ballAnimation = Parabolic(tween(500)),
        indentAnimation = Height(tween(500))
    ) {
        tabs.forEach { tab ->
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tab.title,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }
}

/* ----------------------- PLACEHOLDERS ----------------------- */

@Composable
fun SecretFolderScreen() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Secret Folder", color = Color.White)
    }
}

@Composable
fun ProfileScreen() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Profile", color = Color.White)
    }
}
