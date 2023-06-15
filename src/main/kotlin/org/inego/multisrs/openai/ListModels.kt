package org.inego.multisrs.openai

import com.aallam.openai.client.OpenAI

suspend fun main() {
    val apiKey = System.getenv("API_KEY")

    val openAI = OpenAI(apiKey)

    openAI.models().forEach {
        println(it.id)
    }
}