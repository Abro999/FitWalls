package com.fitwalls.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fitwalls.app.data.FirestoreManager
import com.fitwalls.app.data.Wallpaper
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToGenerator: () -> Unit) {
    val firestoreManager = remember { FirestoreManager() }
    var wallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        wallpapers = firestoreManager.getWallpapers()
        isLoading = false
    }
    
    // TODO: Implement actual premium check. For now, stubbed to false.
    val isPremiumUser = false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FitWalls") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToGenerator) {
                Icon(Icons.Default.Add, contentDescription = "Generate AI Wallpaper")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (wallpapers.isEmpty()) {
                Text(
                    "No wallpapers yet. Be the first to generate one!",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(wallpapers) { wallpaper ->
                            WallpaperCard(wallpaper)
                        }
                    }
                    
                    if (!isPremiumUser) {
                        // TEST Banner Ad Unit ID. Replace with real AdMob Ad Unit ID before release
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(wallpaper: Wallpaper) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.imageUrl,
                contentDescription = wallpaper.prompt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Add gradient or text overlay here if desired
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Text(
                    text = wallpaper.creatorName,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

