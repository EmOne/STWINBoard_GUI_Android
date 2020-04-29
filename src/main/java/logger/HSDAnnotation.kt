package logger

data class HSDAnnotation(
        var id: Int,
        var label: String,
        var pinDesc: String?,
        var tagType: TagType,
        var isSelected:Boolean = false,
        var isEditable:Boolean = false) {
    enum class TagType {
        HW, SW
    } //TagType
}