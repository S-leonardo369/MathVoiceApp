package com.mathvoiceapp.voice

import kotlin.math.abs

object AnswerEvaluator {

    /**
     * Main evaluation entry point.
     * Returns true if the user's spoken/typed answer matches the correct answer.
     */
    fun evaluate(rawUserAnswer: String, correctAnswer: String, acceptedAnswers: List<String>): Boolean {
        val normalizedUser = normalize(rawUserAnswer)

        // 1. Check against the canonical answer
        if (fuzzyMatch(normalizedUser, normalize(correctAnswer))) return true

        // 2. Check against all accepted forms
        for (accepted in acceptedAnswers) {
            if (fuzzyMatch(normalizedUser, normalize(accepted))) return true
        }

        // 3. Numerical comparison (handles "0.5" == "1/2" etc.)
        val userNum = tryEvalSimple(normalizedUser)
        val correctNum = tryEvalSimple(normalize(correctAnswer))
        if (userNum != null && correctNum != null) {
            if (abs(userNum - correctNum) < 0.001) return true
        }

        // 4. Check if correct answer is contained in the user's answer (for multi-part)
        val normCorrect = normalize(correctAnswer)
        if (normalizedUser.length > 2 && normalizedUser.contains(normCorrect)) return true

        return false
    }

    /**
     * Normalize a math answer string:
     * - lowercase
     * - spoken words → symbols/digits
     * - remove redundant spaces
     */
    fun normalize(raw: String): String {
        var s = raw.lowercase().trim()

        // ── Spoken number words → digits ────────────────────────────────
        val numberWords = linkedMapOf(
            "nineteen" to "19", "eighteen" to "18", "seventeen" to "17",
            "sixteen" to "16", "fifteen" to "15", "fourteen" to "14",
            "thirteen" to "13", "twelve" to "12", "eleven" to "11",
            "twenty-one" to "21", "twenty-two" to "22", "twenty-three" to "23",
            "twenty-four" to "24", "twenty-five" to "25", "twenty-six" to "26",
            "twenty-seven" to "27", "twenty-eight" to "28", "twenty-nine" to "29",
            "thirty-one" to "31", "forty-two" to "42", "fifty-five" to "55",
            "sixty-four" to "64",
            "one hundred" to "100", "two hundred" to "200",
            "one thousand" to "1000",
            "hundred twenty" to "120",
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3",
            "four" to "4", "five" to "5", "six" to "6", "seven" to "7",
            "eight" to "8", "nine" to "9", "ten" to "10",
            "twenty" to "20", "thirty" to "30", "forty" to "40",
            "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90", "hundred" to "100",
            "thousand" to "1000"
        )
        for ((word, digit) in numberWords) {
            s = s.replace(word, digit)
        }

        // ── Math vocabulary → symbols ───────────────────────────────────
        s = s
            .replace("does not exist", "dne")
            .replace("doesn't exist", "dne")
            .replace("undefined", "undefined")
            .replace("positive infinity", "∞")
            .replace("negative infinity", "-∞")
            .replace("infinity", "∞")
            .replace("infinite", "∞")
            .replace("euler's number", "e")
            .replace("euler number", "e")
            .replace("pi", "π")
            .replace("plus or minus", "±")
            .replace("positive or negative", "±")
            .replace("plus-minus", "±")
            .replace("x equals", "x=")
            .replace("x equal to", "x=")
            .replace("y equals", "y=")
            .replace("k equals", "k=")
            .replace("equals", "=")
            .replace("equal to", "=")
            .replace(" to the power of ", "^")
            .replace(" to the power ", "^")
            .replace(" squared", "^2")
            .replace(" cubed", "^3")
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^4", "⁴")
            .replace("square root of", "√")
            .replace("square root", "√")
            .replace("sqrt", "√")
            .replace("cube root of", "∛")
            .replace("cube root", "∛")
            .replace("natural log of", "ln")
            .replace("natural log", "ln")
            .replace("log of", "log")
            .replace("sin squared", "sin²")
            .replace("cos squared", "cos²")
            .replace("sec squared", "sec²")
            .replace("x squared", "x²")
            .replace("x cubed", "x³")
            .replace("multiplied by", "*")
            .replace(" times ", "*")
            .replace(" divided by ", "/")
            .replace(" over ", "/")
            .replace(" plus ", "+")
            .replace(" minus ", "-")
            .replace("negative ", "-")
            .replace("positive ", "")
            .replace("half", "1/2")
            .replace("a half", "1/2")
            .replace("a third", "1/3")
            .replace("a quarter", "1/4")
            .replace("two thirds", "2/3")
            .replace("three quarters", "3/4")
            .replace("three halves", "3/2")
            .replace("one and a half", "3/2")
            .replace(" and ", " ")

        // ── Fraction spoken as "N over D" ───────────────────────────────
        s = s.replace(Regex("(\\d+) over (\\d+)")) { mr ->
            "${mr.groupValues[1]}/${mr.groupValues[2]}"
        }

        // ── Remove all spaces around math operators ─────────────────────
        s = s.replace(" + ", "+").replace("+ ", "+").replace(" +", "+")
        s = s.replace(" - ", "-").replace("- ", "-").replace(" -", "-")
        s = s.replace(" * ", "*").replace("× ", "*").replace(" ×", "*").replace("×", "*")
        s = s.replace(" / ", "/")

        // ── Collapse whitespace ─────────────────────────────────────────
        s = s.replace(Regex("\\s+"), " ").trim()

        return s
    }

    /**
     * Fuzzy string matching – strips spaces and compares.
     */
    private fun fuzzyMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val aClean = a.replace(" ", "").replace(",", "")
        val bClean = b.replace(" ", "").replace(",", "")
        return aClean == bClean
    }

    /**
     * Try to evaluate simple numeric expressions including fractions.
     */
    private fun tryEvalSimple(s: String): Double? {
        // Plain double
        s.toDoubleOrNull()?.let { return it }

        // Simple fraction "a/b"
        val fractionRegex = Regex("^(-?\\d+)/(-?\\d+)$")
        fractionRegex.matchEntire(s)?.let { mr ->
            val num = mr.groupValues[1].toDoubleOrNull() ?: return null
            val den = mr.groupValues[2].toDoubleOrNull() ?: return null
            if (den == 0.0) return null
            return num / den
        }

        // Decimal percentage "50%" → 50
        if (s.endsWith("%")) {
            return s.dropLast(1).toDoubleOrNull()
        }

        return null
    }

    /**
     * Post-process raw speech specifically for math:
     * Returns a cleaned string ready for evaluate().
     */
    fun processMathSpeech(rawSpeech: String): String = normalize(rawSpeech)
}
