package com.eyalm.adns.data.nextdns.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class ListIcon {
    data class Url(val url: String) : ListIcon()
    data class Vector(val imageVector: ImageVector) : ListIcon()
    data class Text(val text: String) : ListIcon()
    object None : ListIcon()
}
