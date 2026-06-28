package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.ListIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveListItem(
    onClick: () -> Unit,
    isSelected: Boolean = false,
    altLeadingContent: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    altContent: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    altIconUrl: String? = null,
    secondIcon: ImageVector? = null,
    interactiveItem: (@Composable (isSelected: Boolean, onClick: () -> Unit) -> Unit)? = null,
    title: String,
    description: String? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    stickIcon: Boolean = false,
    iconSize: Dp = 36.dp
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val itemColors = ListItemDefaults.colors(containerColor = containerColor)

    val itemShape = remember(isFirst, isLast) {
        RoundedCornerShape(
            topStart = if (isFirst) 12.dp else 0.dp,
            topEnd = if (isFirst) 12.dp else 0.dp,
            bottomStart = if (isLast) 12.dp else 0.dp,
            bottomEnd = if (isLast) 12.dp else 0.dp
        )
    }

    val itemShapes = ListItemDefaults.shapes(shape = itemShape)

    val leading = remember(icon, altLeadingContent, altIconUrl, isSelected, stickIcon, iconSize) {
        @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    ExpressiveIcon(icon, Modifier.size(iconSize))
                    Spacer(Modifier.width(4.dp))
                }
                if (altLeadingContent != null) {
                    Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                        altLeadingContent(isSelected)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (altIconUrl != null) {
                    ListIconView(
                        icon = ListIcon.Url(altIconUrl),
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }

    val supportingTextStyle = MaterialTheme.typography.bodyMedium
    val supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val supporting = remember(description, supportingTextStyle, supportingTextColor, altContent) {
        @Composable {
            Column {
                description?.let {
                    Text(
                        text = it,
                        style = supportingTextStyle,
                        color = supportingTextColor
                    )
                }
                if (altContent != null) {
                    altContent()
                }
            }

        }
    }

    val titleTextStyle = MaterialTheme.typography.titleMedium
    val titleTextColor = MaterialTheme.colorScheme.onSurface
    val mainContent = remember(title, titleTextStyle, titleTextColor) {
        @Composable {
            Text(
                text = title,
                style = titleTextStyle.copy(fontWeight = FontWeight.Bold),
                color = titleTextColor
            )
        }
    }

    val trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trailing = remember(secondIcon, interactiveItem, isSelected, onClick, trailingIconColor) {
        @Composable {
            if (secondIcon != null) {
                Icon(
                    imageVector = secondIcon,
                    contentDescription = null,
                    tint = trailingIconColor.copy(alpha = 0.7f)
                )
            }
            if (interactiveItem != null) {
                Row() {
                    interactiveItem(isSelected, onClick)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

        }
    }


    SegmentedListItem(
        selected = isSelected,
        colors = itemColors,
        onClick = onClick,
        verticalAlignment = if (stickIcon) Alignment.Top else Alignment.CenterVertically,
        shapes = itemShapes,
        leadingContent = if (stickIcon) null else leading,
        trailingContent = trailing,
        supportingContent = supporting,
        content = {
            if (stickIcon) {
                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                    mainContent()
                }
            } else {
                mainContent()
            }
        }
    )
}
