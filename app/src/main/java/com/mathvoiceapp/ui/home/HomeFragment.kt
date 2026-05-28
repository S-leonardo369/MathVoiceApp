package com.mathvoiceapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mathvoiceapp.R
import com.mathvoiceapp.data.Difficulty
import com.mathvoiceapp.data.Topic
import com.mathvoiceapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedTopic: Topic = Topic.ARITHMETIC
    private var selectedDifficulty: Difficulty = Difficulty.EASY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopicCards()
        setupDifficultyButtons()
        setupStartButton()
    }

    private fun setupTopicCards() {
        val topicViews = mapOf(
            Topic.ARITHMETIC      to binding.cardArithmetic,
            Topic.ALGEBRA         to binding.cardAlgebra,
            Topic.GEOMETRY        to binding.cardGeometry,
            Topic.TRIGONOMETRY    to binding.cardTrigonometry,
            Topic.FUNCTIONS       to binding.cardFunctions,
            Topic.LIMITS          to binding.cardLimits,
            Topic.DIFFERENTIATION to binding.cardDifferentiation,
            Topic.INTEGRATION     to binding.cardIntegration
        )

        topicViews.forEach { (topic, card) ->
            card.setOnClickListener { selectTopic(topic, topicViews) }
        }
        selectTopic(selectedTopic, topicViews)
    }

    private fun selectTopic(topic: Topic, topicViews: Map<Topic, CardView>) {
        selectedTopic = topic
        topicViews.forEach { (t, card) ->
            card.alpha = if (t == topic) 1.0f else 0.55f
            card.cardElevation = if (t == topic) 12f else 2f
        }
        updateDifficultyAvailability()
    }

    private fun updateDifficultyAvailability() {
        val calcTopics = listOf(Topic.LIMITS, Topic.DIFFERENTIATION, Topic.INTEGRATION)
        val isCalc = selectedTopic in calcTopics
        binding.btnEasy.isEnabled = !isCalc
        binding.btnEasy.alpha = if (isCalc) 0.35f else 1.0f
        if (isCalc && selectedDifficulty == Difficulty.EASY) {
            selectedDifficulty = Difficulty.MEDIUM
            highlightDifficulty(Difficulty.MEDIUM)
        }
    }

    private fun setupDifficultyButtons() {
        binding.btnEasy.setOnClickListener   { selectedDifficulty = Difficulty.EASY;   highlightDifficulty(Difficulty.EASY) }
        binding.btnMedium.setOnClickListener { selectedDifficulty = Difficulty.MEDIUM; highlightDifficulty(Difficulty.MEDIUM) }
        binding.btnHard.setOnClickListener   { selectedDifficulty = Difficulty.HARD;   highlightDifficulty(Difficulty.HARD) }
        highlightDifficulty(selectedDifficulty)
    }

    private fun highlightDifficulty(difficulty: Difficulty) {
        val active   = ContextCompat.getColor(requireContext(), R.color.accent_blue)
        val inactive = ContextCompat.getColor(requireContext(), R.color.card_surface)
        val white    = ContextCompat.getColor(requireContext(), R.color.white)
        val grey     = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        listOf(
            Difficulty.EASY   to binding.btnEasy,
            Difficulty.MEDIUM to binding.btnMedium,
            Difficulty.HARD   to binding.btnHard
        ).forEach { (d, btn) ->
            val sel = (d == difficulty)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (sel) active else inactive)
            btn.setTextColor(if (sel) white else grey)
        }
    }

    private fun setupStartButton() {
        binding.btnStart.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToQuizFragment(
                topic      = selectedTopic,
                difficulty = selectedDifficulty
            )
            findNavController().navigate(action)
        }

        binding.btnMixedMode.setOnClickListener {
            // Mixed mode: pick a random topic, use Hard (includes all sub-difficulties)
            val randomTopic = Topic.values().toList().shuffled().first()
            val action = HomeFragmentDirections.actionHomeFragmentToQuizFragment(
                topic      = randomTopic,
                difficulty = Difficulty.HARD
            )
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
