package org.inego.multisrs.openai.utils

import org.inego.multisrs.openai.data.Word
import kotlin.random.Random


fun weightedNRandom(n: Int, elements: List<Word>): List<Word> {
    var current = elements
    val result = mutableListOf<Word>()
    for (i in 0 until n) {
        val nextRandom = weightedRandom(current) ?: break
        result += nextRandom
        current = current - nextRandom
    }
    return result
}


fun weightedRandom(words: List<Word>): Word? {
    if (words.isEmpty()) {
        return null
    }

    val max = words.maxOf { it.lastSeen } + 1

    // Sum of first n numbers = n * (n+1) / 2
    val totalWeight = words.sumOf { max - it.lastSeen }
    var randomWeight = Random.nextLong(totalWeight) + 1

    for (word in words) {
        val weight = max - word.lastSeen
        if (randomWeight <= weight) {
            return word
        }
        randomWeight -= weight
    }

    return null
}


