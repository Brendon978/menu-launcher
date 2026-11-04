package com.brendon978.menulauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    entry: AppEntry,
    override: String?,
    color: Int,
    renderer: IconRenderer,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    tracksBadge: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AppIcon(entry, override, color, 24.dp, renderer, tracksBadge)
        Spacer(Modifier.width(14.dp))
        Text(entry.label, color = Color(color), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
