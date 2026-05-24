package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveListItem(
    onClick: () -> Unit,
    isSelected: Boolean = false,
    icon: ImageVector? = null,
    secondIcon: ImageVector? = null,
    interactiveItem: (@Composable (isSelected: Boolean, onClick: () -> Unit) -> Unit)? = null,
    title: String,
    description: String?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
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

    val leading = remember(icon) {
        icon?.let {
            @Composable {
                Row {
                    ExpressiveIcon(it)
                    Spacer(Modifier.width(4.dp))
                }

            }
        }
    }

    val supportingTextStyle = MaterialTheme.typography.bodyMedium
    val supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val supporting = remember(description, supportingTextStyle, supportingTextColor) {
        description?.let {
            @Composable {
                Text(
                    text = it,
                    style = supportingTextStyle,
                    color = supportingTextColor
                )
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
                interactiveItem(isSelected, onClick)
            }
        }
    }


    SegmentedListItem(
        selected = isSelected,
        colors = itemColors,
        onClick = onClick,
        verticalAlignment = Alignment.CenterVertically,
        shapes = itemShapes,
        leadingContent = leading,
        trailingContent = trailing,
        supportingContent = supporting,
        content = mainContent
    )
}
