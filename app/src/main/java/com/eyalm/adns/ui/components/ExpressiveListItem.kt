package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.nextdns.model.ListIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveListItem(
    onClick: () -> Unit = {},
    isSelected: Boolean = false,
    overrideCorners: Boolean = false,
    altLeadingContent: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    icon: ImageVector? = null,
    altIconUrl: String? = null,
    secondIcon: ImageVector? = null,
    interactiveItem: (@Composable (isSelected: Boolean, onClick: () -> Unit) -> Unit)? = null,
    title: String,
    description: String? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    stickIcon: Boolean = false,
    trailingCenter: Boolean = false,
    iconSize: Dp = 36.dp,
    indicatorColor: Color? = null,
    topPadding: Dp = 0.dp,
    altContent: (@Composable () -> Unit)? = null
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

    val itemShapes = ListItemDefaults.shapes(
        shape = itemShape,
        selectedShape = if (overrideCorners) itemShape else null
    )

    val leading = remember(icon, altLeadingContent, altIconUrl, isSelected, stickIcon, iconSize, topPadding) {
        @Composable {
            Row(
                modifier = Modifier.padding(top = topPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

    val noLeading = icon == null && altLeadingContent == null && altIconUrl == null
    val supportingTextStyle = MaterialTheme.typography.bodyMedium
    val supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val supporting = remember(description, supportingTextStyle, supportingTextColor, altContent, noLeading) {
        @Composable {
            Column(modifier = Modifier.padding(end = if (noLeading) 12.dp else 0.dp)) {
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
    val mainContent = remember(title, titleTextStyle, titleTextColor, topPadding, noLeading) {
        @Composable {
            Text(
                text = title,
                style = titleTextStyle.copy(fontWeight = FontWeight.Bold),
                color = titleTextColor,
                modifier = Modifier.padding(top = topPadding, end = if (noLeading) 12.dp else 0.dp)
            )
        }
    }

    val trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trailing = remember(secondIcon, interactiveItem, isSelected, onClick, trailingIconColor, topPadding) {
        if (secondIcon != null || interactiveItem != null) {
            @Composable {
                Row(
                    modifier = Modifier.padding(top = topPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (secondIcon != null) {
                        Icon(
                            imageVector = secondIcon,
                            contentDescription = null,
                            tint = trailingIconColor.copy(alpha = 0.7f)
                        )
                    }
                    if (interactiveItem != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            interactiveItem(isSelected, onClick)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        } else {
            null
        }
    }


    SegmentedListItem(
        selected = isSelected,
        colors = itemColors,
        onClick = onClick,
        verticalAlignment = if (stickIcon || trailingCenter) Alignment.Top else Alignment.CenterVertically,
        shapes = itemShapes,
        leadingContent = if (stickIcon || trailingCenter) null else leading,
        trailingContent = if (trailingCenter) null else trailing,
        supportingContent = if (trailingCenter) {
            altContent?.let {
                {
                    Column { it() }
                }
            }
        } else {
            supporting
        },
        modifier = Modifier
            .clip(itemShape)
            .then(
                if (indicatorColor != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        val widthPx = 4.dp.toPx()
                        drawRect(
                            color = indicatorColor,
                            topLeft = Offset.Zero,
                            size = Size(widthPx, size.height)
                        )
                    }
                } else Modifier
            ),
        content = {
            if (trailingCenter) {
                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp).padding(end = if (noLeading) 12.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        mainContent()
                        description?.let {
                            Text(
                                text = it,
                                style = supportingTextStyle,
                                color = supportingTextColor
                            )
                        }
                    }
                    trailing?.invoke()
                }
            } else if (stickIcon) {
                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp).padding(end = if (noLeading) 12.dp else 0.dp),
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
