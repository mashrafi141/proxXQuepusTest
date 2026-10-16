package com.example.model

enum class MediaType {
    VIDEO,
    PHOTO,
    GIF,
    AUDIO,
    OTHER
}

data class Breadcrumb(
    val title: String,
    val uriString: String
)

enum class SortField {
    NAME,
    DATE,
    SIZE
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}
