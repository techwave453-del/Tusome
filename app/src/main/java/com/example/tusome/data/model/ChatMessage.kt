package com.example.tusome.data.model

enum class MessageSender {
    USER,
    AI_TUTOR
}

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis()
)