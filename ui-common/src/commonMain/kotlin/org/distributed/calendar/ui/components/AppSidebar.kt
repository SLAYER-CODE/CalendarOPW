package org.distributed.calendar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SidebarTab(
    val id: String,
    val label: String,
    val emoji: String
)

@Composable
fun AppSidebar(
    tabs: List<SidebarTab>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 64.dp,
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .background(Color(0xFF7B1FA2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isExpanded) 16.dp else 8.dp, vertical = 16.dp)
        ) {
            ToggleButton(isExpanded = isExpanded, onToggle = onToggle)

            Spacer(Modifier.weight(0.3f))

            tabs.forEach { tab ->
                NavItem(
                    tab = tab,
                    isSelected = tab.id == selectedTabId,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(tab.id) }
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "v1.0",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleButton(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = if (isHovered) Color.White.copy(alpha = 0.12f) else Color.Transparent

    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(onClick = onToggle)
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☰", color = Color.White, fontSize = 20.sp)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Calendar",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(onClick = onToggle)
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("☰", color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
private fun NavItem(
    tab: SidebarTab,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        isSelected -> Color.White.copy(alpha = 0.25f)
        isHovered -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(onClick = onClick)
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tab.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = tab.label,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(onClick = onClick)
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(tab.emoji, fontSize = 20.sp)
        }
    }
}
