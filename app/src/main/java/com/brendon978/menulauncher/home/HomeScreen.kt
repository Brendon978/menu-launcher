package com.brendon978.menulauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0C0C0E))) {
        Text(
            "menu-launcher",
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.statusBarsPadding().padding(22.dp),
        )
    }
}
