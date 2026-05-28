package com.mathvoiceapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class MathQuestion(
    val id: String,
    val topic: Topic,
    val difficulty: Difficulty,
    val question: String,
    val answer: String,
    val acceptedAnswers: List<String>,
    val hint: String = "",
    val explanation: String = ""
)

@Parcelize
enum class Topic(val displayName: String, val emoji: String) : Parcelable {
    ARITHMETIC("Arithmetic", "🔢"),
    ALGEBRA("Algebra", "x²"),
    GEOMETRY("Geometry", "📐"),
    TRIGONOMETRY("Trigonometry", "sinθ"),
    FUNCTIONS("Functions", "f(x)"),
    LIMITS("Limits", "lim"),
    DIFFERENTIATION("Differentiation", "d/dx"),
    INTEGRATION("Integration", "∫")
}

@Parcelize
enum class Difficulty(val displayName: String) : Parcelable {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}
