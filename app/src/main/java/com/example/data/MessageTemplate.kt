package com.example.data

data class MessageTemplate(
    val id: String,
    val name: String,
    val category: String,
    val defaultText: String,
    val supportedPlaceholders: List<String>,
    val description: String = "",
    val sampleData: Map<String, String> = emptyMap()
)
