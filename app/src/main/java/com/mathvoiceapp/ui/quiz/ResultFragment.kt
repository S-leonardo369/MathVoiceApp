package com.mathvoiceapp.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mathvoiceapp.R
import com.mathvoiceapp.databinding.FragmentResultBinding

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val score = args.score
        val total = args.total
        val pct   = if (total > 0) (score * 100) / total else 0

        binding.tvScore.text   = "$score / $total"
        binding.tvPercent.text = "$pct%"
        binding.tvMessage.text = when {
            pct == 100 -> "Perfect! 🎉 You're a math genius!"
            pct >= 80  -> "Excellent! 🌟 Keep it up!"
            pct >= 60  -> "Good job! 👍 Practice makes perfect."
            pct >= 40  -> "Not bad! 📚 Review and try again."
            else       -> "Keep practising! 💪 You'll get there."
        }
        binding.progressCircle.progress = pct

        binding.btnPlayAgain.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnHome.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
