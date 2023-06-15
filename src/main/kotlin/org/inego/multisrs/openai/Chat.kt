package org.inego.multisrs.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import org.slf4j.LoggerFactory

@OptIn(BetaOpenAI::class)
suspend fun main() {

    val logger = LoggerFactory.getLogger("ChatKt")

    val apiKey = System.getenv("API_KEY")

    println("Creating the client...")


    val openAI = OpenAI(
        apiKey,
        logging = LoggingConfig(logger = Logger.Default)
    )

    println("Getting the model...")

    val modelId = ModelId("gpt-3.5-turbo")

    println("Calling chat completion...")

    val completion = openAI.chatCompletion(
        ChatCompletionRequest(
            modelId,
            listOf(
                ChatMessage(
                    ChatRole.User,
                    "Write a paragraph in Serbian with \"momak\", \"upasti u nevolju\", \"predsednik\""
                )
            )
        )
    )

    println(completion.usage?.totalTokens)
    println(completion.choices[0].message)

}