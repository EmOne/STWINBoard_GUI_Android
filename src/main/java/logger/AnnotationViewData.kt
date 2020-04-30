package logger

import androidx.annotation.StringRes

internal data class AnnotationViewData(
        val id: Int,
        var label: String,
        var pinDesc: String?,
        @StringRes val tagType: Int,
        var isSelected:Boolean = false,
        var isEditable:Boolean = false,
        var isSelectable:Boolean = false)