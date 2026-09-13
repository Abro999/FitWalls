package com.fitwalls.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.fitwalls.app.R
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Upload

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import com.fitwalls.app.util.WALLPAPER_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToGenerator: () -> Unit, onNavigateToUpload: () -> Unit, onNavigateToPreview: (String) -> Unit) {
    val firestoreManager = remember { FirestoreManager() }
    var wallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All") + WALLPAPER_CATEGORIES }
    
    val filteredWallpapers = remember(wallpapers, selectedCategory) {
        if (selectedCategory == "All") wallpapers else wallpapers.filter { it.style == selectedCategory }
    }
    
    var userRole by remember { mutableStateOf<String?>("user") }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLoading = true
                // Launch coroutine to fetch wallpapers and role
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    wallpapers = firestoreManager.getWallpapers()
                    userRole = firestoreManager.getUserRole()
                    isLoading = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // TODO: Implement actual premium check. For now, stubbed to false.
    val isPremiumUser = false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fitwalls_logo),
                            contentDescription = "FitWalls Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("FitWalls")
                    }
                },
                actions = {
                    if (userRole == "creator") {
                        IconButton(onClick = onNavigateToUpload) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Wallpaper")
                        }
                    }
                }
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
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        lazyItems(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    
                    if (filteredWallpapers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                "No wallpapers in this category yet.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredWallpapers) { wallpaper ->
                                WallpaperCard(wallpaper, onClick = { onNavigateToPreview(wallpaper.id) })
                            }
                        }
                    }
                    
                    if (!isPremiumUser) {
                        // REAL Banner Ad Unit ID: ca-app-pub-6916958158520465/1961203482
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-6916958158520465/1961203482"
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
fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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

