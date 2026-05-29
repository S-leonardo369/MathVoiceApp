package com.mathvoiceapp.ui.quiz

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.mathvoiceapp.R
import com.mathvoiceapp.databinding.FragmentQuizBinding
import com.mathvoiceapp.voice.AnswerEvaluator
import com.mathvoiceapp.voice.VoiceRecognitionManager

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizFragmentArgs by navArgs()

    private lateinit var voiceManager: VoiceRecognitionManager
    private var pulseAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceListening()
        else Snackbar.make(binding.root, "Microphone permission is needed for voice answers.", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVoiceManager()
        setupObservers()
        setupClickListeners()
        viewModel.startQuiz(args.topic, args.difficulty)
    }

    // ── Voice Setup ────────────────────────────────────────────────────────

    private fun setupVoiceManager() {
        voiceManager = VoiceRecognitionManager(requireContext()).apply {
            onReadyForSpeech = {
                activity?.runOnUiThread {
                    binding.tvVoiceStatus.text = "🎙 Listening…"
                    startPulseAnimation()
                }
            }
            onBeginningOfSpeech = {
                activity?.runOnUiThread {
                    binding.tvVoiceStatus.text = "Speaking detected…"
                }
            }
            onPartialResult = { partial ->
                activity?.runOnUiThread {
                    viewModel.updatePartialVoiceText(partial)
                    binding.tvVoiceInput.text = partial
                }
            }
            onResult = { result ->
                activity?.runOnUiThread {
                    stopPulseAnimation()
                    binding.tvVoiceStatus.text = "Got it!"
                    val displayed = AnswerEvaluator.processMathSpeech(result)
                    binding.tvVoiceInput.text = displayed
                    viewModel.submitAnswer(result)
                }
            }
            onEndOfSpeech = {
                activity?.runOnUiThread {
                    stopPulseAnimation()
                }
            }
            onError = { code, msg ->
                activity?.runOnUiThread {
                    stopPulseAnimation()
                    when (code) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            binding.tvVoiceStatus.text = "Didn't catch that — tap to retry"
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ startVoiceListening() }, 600)
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            binding.tvVoiceStatus.text = "Need microphone permission"
                        }
                        else -> binding.tvVoiceStatus.text = msg
                    }
                }
            }
        }
    }

    // ── Observers ──────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.quizState.observe(viewLifecycleOwner) { state ->
            if (state.isFinished) {
                navigateToResult(state.score, state.totalQuestions)
                return@observe
            }

            binding.tvQuestion.text    = state.question.question
            binding.tvProgress.text    = "${state.questionNumber} / ${state.totalQuestions}"
            binding.tvScore.text       = "Score: ${state.score}"
            binding.progressBar.max    = state.totalQuestions
            binding.progressBar.progress = state.questionNumber

            when (state.answerState) {
                AnswerState.NONE    -> showAnsweringUI()
                AnswerState.CORRECT -> showFeedback(correct = true,  state.question.answer, state.question.explanation)
                AnswerState.WRONG   -> showFeedback(correct = false, state.question.answer, state.question.explanation)
            }
        }

        viewModel.voiceInputText.observe(viewLifecycleOwner) { text ->
            if (text.isNotBlank()) binding.tvVoiceInput.text = text
        }
    }

    // ── Click Listeners ────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnMic.setOnClickListener {
            if (viewModel.quizState.value?.answerState != AnswerState.NONE) return@setOnClickListener
            if (voiceManager.isCurrentlyListening()) {
                voiceManager.stopListening()
                stopPulseAnimation()
                binding.tvVoiceStatus.text = "Tap mic to answer"
            } else {
                checkPermissionAndListen()
            }
        }

        binding.btnNext.setOnClickListener {
            viewModel.nextQuestion()
            resetAnswerUI()
        }

        binding.btnHint.setOnClickListener {
            val hint = viewModel.quizState.value?.question?.hint
            val msg = if (!hint.isNullOrBlank()) "💡 $hint" else "No hint for this question."
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
        }

        binding.btnBack.setOnClickListener {
            voiceManager.destroy()
            findNavController().popBackStack()
        }
    }

    // ── Voice Permission + Start ───────────────────────────────────────────

    private fun checkPermissionAndListen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> startVoiceListening()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceListening() {
        binding.tvVoiceInput.text = ""
        binding.tvVoiceStatus.text = "Starting…"
        voiceManager.startListening()
    }

    // ── Animations ─────────────────────────────────────────────────────────

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.3f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val s = it.animatedValue as Float
                binding.btnMic.scaleX = s
                binding.btnMic.scaleY = s
            }
            start()
        }
        binding.btnMic.setBackgroundResource(R.drawable.mic_listening_bg)
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.btnMic.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        binding.btnMic.setBackgroundResource(R.drawable.mic_idle_bg)
    }

    private fun animateFeedbackCard(correct: Boolean) {
        val color = if (correct)
            ContextCompat.getColor(requireContext(), R.color.correct_green)
        else
            ContextCompat.getColor(requireContext(), R.color.wrong_red)

        binding.cardFeedback.setCardBackgroundColor(color)
        binding.cardFeedback.alpha = 0f
        binding.cardFeedback.visibility = View.VISIBLE
        binding.cardFeedback.animate().alpha(1f).setDuration(300).start()

        // Pop icon
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvFeedbackIcon, "scaleX", 0f, 1.3f, 1f),
                ObjectAnimator.ofFloat(binding.tvFeedbackIcon, "scaleY", 0f, 1.3f, 1f)
            )
            duration = 400
            start()
        }
    }

    // ── UI State ───────────────────────────────────────────────────────────

    private fun showAnsweringUI() {
        binding.cardFeedback.visibility = View.GONE
        binding.btnNext.visibility      = View.GONE
        binding.btnMic.isEnabled        = true
        binding.tvVoiceStatus.text      = "Tap mic to answer"
        binding.tvVoiceInput.text       = ""
    }

    private fun showFeedback(correct: Boolean, correctAnswer: String, explanation: String) {
        voiceManager.stopListening()
        stopPulseAnimation()

        binding.tvFeedbackIcon.text  = if (correct) "✓" else "✗"
        binding.tvFeedbackLabel.text = if (correct) "Correct! 🎉" else "Incorrect"
        binding.tvCorrectAnswer.text = if (!correct) "Answer: $correctAnswer" else ""
        binding.tvCorrectAnswer.visibility = if (!correct) View.VISIBLE else View.GONE
        binding.tvExplanation.text = explanation
        binding.tvExplanation.visibility = if (explanation.isNotBlank()) View.VISIBLE else View.GONE

        animateFeedbackCard(correct)
        binding.btnNext.visibility = View.VISIBLE
        binding.btnMic.isEnabled   = false
        binding.tvVoiceStatus.text = if (correct) "Nailed it! 🌟" else "Keep going! 💪"
    }

    private fun resetAnswerUI() {
        binding.cardFeedback.visibility = View.GONE
        binding.btnNext.visibility      = View.GONE
        binding.tvVoiceInput.text       = ""
        binding.tvVoiceStatus.text      = "Tap mic to answer"
        binding.btnMic.isEnabled        = true
    }

    private fun navigateToResult(score: Int, total: Int) {
        findNavController().navigate(
            QuizFragmentDirections.actionQuizFragmentToResultFragment(score, total)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)   // Bug 3 fix: cancel any pending postDelayed
        voiceManager.destroy()
        _binding = null
    }
}
