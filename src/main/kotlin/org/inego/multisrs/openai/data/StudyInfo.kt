package org.inego.multisrs.openai.data

import kotlinx.serialization.Serializable
import org.inego.multisrs.openai.WorkingListWord
import org.inego.multisrs.openai.utils.weightedNRandom

@Serializable
data class StudyInfo(
    val language: String,
    val step: Long,
    val words: List<Word>
) {
    fun add(word: String): StudyInfo {
        return StudyInfo(language, step, words + Word(word, true, 0))
    }

    fun apply(currentWords: List<WorkingListWord>): StudyInfo =
        StudyInfo(
            language,
            step,
            words.map { word ->
                val currentWord = currentWords.find { it.word == word.text }
                if (currentWord == null)
                    word
                else
                    Word(word.text, currentWord.focused, step)
            }
        )

    fun inc(newWords: List<Word>): StudyInfo {
        val newStep = step + 1

        return StudyInfo(language, newStep, words.map { word ->
            if (word in newWords)
                Word(word.text, word.focused, newStep)
            else
                word
        })
    }

    fun find(word: String): Word? {
        return words.find { it.text == word }
    }

    fun suggest(n: Int): List<Word> {

        var left = n

        val (focused, unfocused) = words.partition { it.focused }

        if (left < focused.size) {
            return weightedNRandom(left, focused)
        }

        val result = mutableListOf<Word>()

        result.addAll(focused)
        left -= focused.size

        result.addAll(weightedNRandom(left, unfocused))

        return result
    }

    fun unfocusAll(): StudyInfo {
        return StudyInfo(language, step, words.map { it.unfocus() })
    }
}
