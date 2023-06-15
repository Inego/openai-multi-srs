package org.inego.multisrs.openai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
class Word(
    val text: String,
    val focused: Boolean,
    val lastSeen: Long
) {
    override fun toString(): String {
        return "'$text' (focused=$focused, lastSeen=$lastSeen)"
    }

    fun unfocus(): Word = if (focused)
        Word(text, false, lastSeen)
    else
        this
}


fun main() {

    println(Json.encodeToString(Word("aha", true, 12)))

}