package com.example.player

enum class GestureMode {
    NONE,
    HORIZONTAL_SEEK
}

data class PlayerGestureFeedback(
    val iconType: FeedbackIcon = FeedbackIcon.NONE,
    val text: String = "",
    val percentage: Float = 0f,
    val isVisible: Boolean = false
)

enum class FeedbackIcon {
    NONE,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    NEXT_MEDIA,
    PREV_MEDIA,
    BRIGHTNESS,
    VOLUME,
    MUTE
}
