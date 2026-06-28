package com.na982.opichelper.domain.usecase

import javax.inject.Inject

data class SentencePair(val korean: String, val english: String)

data class ValidationResult(
    val errors: List<ValidationError>,
    val isValid: Boolean
)

sealed class ValidationError {
    data class MissingPunctuation(val index: Int, val isKorean: Boolean) : ValidationError()
    data class EmptySentence(val index: Int, val isKorean: Boolean) : ValidationError()
}

class ValidateScriptEditUseCase @Inject constructor() {
    fun validate(pairs: List<SentencePair>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        pairs.forEachIndexed { i, pair ->
            if (pair.korean.isBlank())
                errors.add(ValidationError.EmptySentence(i, isKorean = true))
            if (pair.english.isBlank())
                errors.add(ValidationError.EmptySentence(i, isKorean = false))
            if (pair.korean.isNotBlank() && !pair.korean.trim().endsWithSentenceEnd())
                errors.add(ValidationError.MissingPunctuation(i, isKorean = true))
            if (pair.english.isNotBlank() && !pair.english.trim().endsWithSentenceEnd())
                errors.add(ValidationError.MissingPunctuation(i, isKorean = false))
        }
        return ValidationResult(errors, errors.isEmpty())
    }

    private fun String.endsWithSentenceEnd(): Boolean =
        trimEnd().let { text ->
            text.endsWith(".") || text.endsWith("!") || text.endsWith("?") || text.endsWith("\u3002")
        }
}
