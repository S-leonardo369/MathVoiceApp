package com.mathvoiceapp.ui.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mathvoiceapp.data.Difficulty
import com.mathvoiceapp.data.MathQuestion
import com.mathvoiceapp.data.QuestionBank
import com.mathvoiceapp.data.Topic
import com.mathvoiceapp.voice.AnswerEvaluator

enum class AnswerState { NONE, CORRECT, WRONG }

data class QuizState(
    val question: MathQuestion,
    val questionNumber: Int,
    val totalQuestions: Int,
    val score: Int,
    val answerState: AnswerState = AnswerState.NONE,
    val lastUserAnswer: String = "",
    val isFinished: Boolean = false
)

class QuizViewModel : ViewModel() {

    private val _quizState = MutableLiveData<QuizState>()
    val quizState: LiveData<QuizState> = _quizState

    private val _voiceInputText = MutableLiveData<String>()
    val voiceInputText: LiveData<String> = _voiceInputText

    private var questions: List<MathQuestion> = emptyList()
    private var currentIndex = 0
    private var score = 0

    fun startQuiz(topic: Topic, difficulty: Difficulty) {
        // Include questions up to and including the selected difficulty
        val difficulties = when (difficulty) {
            Difficulty.EASY   -> listOf(Difficulty.EASY)
            Difficulty.MEDIUM -> listOf(Difficulty.EASY, Difficulty.MEDIUM)
            Difficulty.HARD   -> listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)
        }

        questions = QuestionBank.getAllForMode(listOf(topic), difficulties)
            .take(15)
            .ifEmpty { QuestionBank.getRandomQuestions(topic, 15) }

        if (questions.isEmpty()) return

        currentIndex = 0
        score = 0
        emitCurrentState(AnswerState.NONE)
    }

    fun submitAnswer(rawAnswer: String) {
        val q = currentQuestion() ?: return
        if (_quizState.value?.answerState != AnswerState.NONE) return  // already answered

        val isCorrect = AnswerEvaluator.evaluate(rawAnswer, q.answer, q.acceptedAnswers)
        if (isCorrect) score++

        _voiceInputText.value = rawAnswer
        emitCurrentState(
            answerState = if (isCorrect) AnswerState.CORRECT else AnswerState.WRONG,
            userAnswer = rawAnswer
        )
    }

    fun nextQuestion() {
        currentIndex++
        if (currentIndex >= questions.size) {
            // Quiz finished
            _quizState.value = _quizState.value?.copy(isFinished = true)
        } else {
            _voiceInputText.value = ""
            emitCurrentState(AnswerState.NONE)
        }
    }

    fun updatePartialVoiceText(text: String) {
        _voiceInputText.value = text
    }

    fun restartQuiz() {
        questions = questions.shuffled()
        currentIndex = 0
        score = 0
        _voiceInputText.value = ""
        emitCurrentState(AnswerState.NONE)
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun currentQuestion() = questions.getOrNull(currentIndex)

    private fun emitCurrentState(answerState: AnswerState, userAnswer: String = "") {
        val q = currentQuestion() ?: return
        _quizState.value = QuizState(
            question       = q,
            questionNumber = currentIndex + 1,
            totalQuestions = questions.size,
            score          = score,
            answerState    = answerState,
            lastUserAnswer = userAnswer,
            isFinished     = false
        )
    }
}
