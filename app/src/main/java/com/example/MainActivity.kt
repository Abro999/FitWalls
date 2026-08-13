package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = BgDark,
          bottomBar = { BottomNav() }
        ) { innerPadding ->
          Column(
            modifier = Modifier
              .padding(innerPadding)
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Header()
            HeroCard()
            AIStudioButton()
            TrendingStyles()
          }
        }
      }
    }
  }
}

@Composable
fun Header() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.linearGradient(listOf(GradientStart, GradientEnd), start = Offset(Float.POSITIVE_INFINITY, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .blur(2.dp)
        )
      }
      Text("FitWalls", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OnBgDark)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      IconButton(
        onClick = { },
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(SurfaceVariantDark)
      ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = OnBgDark)
      }
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(SurfaceDark)
          .border(2.dp, PrimaryPurple, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text("JD", fontWeight = FontWeight.Bold, color = PrimaryPurple)
      }
    }
  }
}

@Composable
fun HeroCard() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(240.dp)
      .clip(RoundedCornerShape(32.dp))
      .background(SurfaceDark)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(listOf(PrimaryPurple.copy(alpha = 0.2f), BgDark, SurfaceVariantDark), start = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), end = Offset(0f, 0f)))
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.8f))))
    )
    
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Box(
          modifier = Modifier
            .background(PrimaryPurple, RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
          Text("FEATURED ARTIST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimaryPurple)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Cyber Punk Dusk", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("by @matsuo_ai", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
      }
      Button(
        onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
      ) {
        Text("Apply", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      }
    }
  }
}

@Composable
fun AIStudioButton() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(80.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(SurfaceVariantDark)
      .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
      .padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(PrimaryPurple, Color(0xFFB69DF8)), start = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), end = Offset(0f, 0f))),
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnPrimaryPurple, modifier = Modifier.size(28.dp))
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text("AI Studio", fontWeight = FontWeight.Bold, color = OnBgDark)
      Text("Generate a custom wallpaper", fontSize = 12.sp, color = OnSurfaceVariantDark)
    }
    Box(
      modifier = Modifier
        .background(OnPrimaryPurple, RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Text("3 CREDITS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
    }
  }
}

@Composable
fun TrendingStyles() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Trending Styles", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBgDark)
      Text("See all", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PrimaryPurple)
    }
    
    Row(
      modifier = Modifier.fillMaxWidth().weight(1f),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(24.dp))
          .background(SurfaceDark)
          .border(1.dp, SurfaceVariantDark, RoundedCornerShape(24.dp))
      ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFD8E4).copy(alpha = 0.15f)))
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
        ) {
          Text("Minimalist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text("1.2k Wallpapers", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
        }
      }
      
      Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceVariantDark, RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.fillMaxSize().background(Color(0xFFD0BCFF).copy(alpha = 0.15f)))
          Text("ABSTRACT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceVariantDark, RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.fillMaxSize().background(Color(0xFFC4EED0).copy(alpha = 0.15f)))
          Text("NATURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
        }
      }
    }
  }
}

@Composable
fun BottomNav() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(80.dp)
      .background(BottomNavBg)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceAround
  ) {
    BottomNavItem(Icons.Default.Home, "Home", true)
    BottomNavItem(Icons.Outlined.Explore, "Explore", false)
    BottomNavItem(Icons.Outlined.Collections, "Studio", false)
    BottomNavItem(Icons.Outlined.Person, "Account", false)
  }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (selected) {
      Box(
        modifier = Modifier
          .width(64.dp)
          .height(32.dp)
          .clip(RoundedCornerShape(percent = 50))
          .background(OnPrimaryPurple),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = label, tint = PrimaryPurple)
      }
      Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PrimaryPurple)
    } else {
      Icon(icon, contentDescription = label, tint = OnBgDark.copy(alpha = 0.6f))
      Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnBgDark.copy(alpha = 0.6f))
    }
  }
}
