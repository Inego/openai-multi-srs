@file:OptIn(BetaOpenAI::class)


package org.inego.multisrs.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.inego.multisrs.openai.data.StudyInfo
import org.inego.multisrs.openai.data.Word
import java.io.File
import kotlin.system.exitProcess

const val N_WORDS = 3

val json = Json { prettyPrint = true }

val apiKey: String = System.getenv("API_KEY")


class StudyProcess(private val studyInfoFileName: String) {

    private var studyInfo = readOrCreate()

    private val language: String
        get() = studyInfo.language

    private var currentWords: List<WorkingListWord> = emptyList()
    private var currentText: String? = null

    init {
        println("Creating the client...")
    }

    private val openAI = OpenAI(
        apiKey,
        logging = LoggingConfig(logger = Logger.Default)
    )

    init {
        println("Getting the model...")
    }


//    private val modelId = ModelId("gpt-3.5-turbo")
    private val modelId = ModelId("gpt-3.5-turbo-0613")

    private fun readOrCreate(): StudyInfo {
        val file = File(studyInfoFileName)
        if (file.exists()) {
            val decoded = json.decodeFromString<StudyInfo>(file.readText(Charsets.UTF_8))
            println("Study info read: ${decoded.words.count()} words")
            return decoded
        }
        print("Enter the language: ")
        val language = readln()
        return StudyInfo(language, 0, listOf())
    }

    private fun save() {
        val serialized = json.encodeToString(studyInfo)
        File(studyInfoFileName).writeText(serialized, Charsets.UTF_8)
    }

    fun add(newWord: String) {
        val found = studyInfo.find(newWord)
        if (found == null) {
            studyInfo = studyInfo.add(newWord)
            save()
        } else {
            println("Already exists! $found")
        }
    }

    suspend fun nextText() {
        if (currentWords.isNotEmpty()) {
            studyInfo = studyInfo.apply(currentWords)
        }

        val suggested = studyInfo.suggest(N_WORDS)

        val startMs = System.currentTimeMillis()

        val completion = requestText(suggested, openAI, modelId)

        val elapsedMs = System.currentTimeMillis() - startMs
        currentText = completion.choices[0].message!!.content
        println()
        println(currentText)
        println()
        println("(Received in $elapsedMs ms. Tokens spent: ${completion.usage!!.totalTokens})")
        println()

        studyInfo = studyInfo.inc(suggested)
        save()

        currentWords = suggested.mapIndexed { index, word -> WorkingListWord(index + 1, word.text, word.focused) }
        printCurrentWords()
    }

    @OptIn(BetaOpenAI::class)
    suspend fun requestExplanation(what: String): ChatCompletion {
        println("Requesting explanation...")

        return openAI.chatCompletion(
            ChatCompletionRequest(
                modelId,
                listOf(
                    ChatMessage(
                        ChatRole.System,
                        "A text in $language"
                    ),
                    ChatMessage(
                        ChatRole.Assistant,
                        currentText!!
                    ),
                    ChatMessage(
                        ChatRole.User,
                        "Explain what \"$what\" means here"
                    )
                )
            )
        )
    }


    @OptIn(BetaOpenAI::class)
    suspend fun translate(): ChatCompletion {
        println("Requesting translation...")

        return openAI.chatCompletion(
            ChatCompletionRequest(
                modelId,
                listOf(
                    ChatMessage(
                        ChatRole.System,
                        "Translate from $language"
                    ),
                    ChatMessage(
                        ChatRole.User,
                        currentText!!
                    )
                )
            )
        )
    }

    private suspend fun requestText(wordList: List<Word>, openAI: OpenAI, modelId: ModelId): ChatCompletion {
        val words = wordList.joinToString(", ") { it.text }
        println("Requesting text...")

        return openAI.chatCompletion(
            ChatCompletionRequest(
                modelId,
                listOf(
                    ChatMessage(
                        ChatRole.System,
                        "Assistant writes a short text in $language with the following words"
                    ),
                    ChatMessage(
                        ChatRole.User,
                        words
                    )
                )
            )
        )
    }


    private fun printCurrentWords() {
        currentWords.forEach {
            println("${it.number}. ${if (it.focused) '*' else ' '} ${it.word}")
        }
    }

    fun toggleWord(lineAsInt: Int) {
        if (lineAsInt < 1 || lineAsInt > currentWords.size) {
            println("Invalid word number")
        } else {
            currentWords = currentWords.map {
                if (it.number == lineAsInt) WorkingListWord(lineAsInt, it.word, !it.focused) else it
            }
            printCurrentWords()
        }
    }

    fun unfocusAll() {
        studyInfo = studyInfo.unfocusAll()
        println("Unfocused all words.")
        save()
        currentWords = emptyList()
    }

    suspend fun explain(what: String) {
        val start = System.currentTimeMillis()
        val explanation = requestExplanation(what)
        val elapsedMs = System.currentTimeMillis() - start
        println()
        println(explanation.choices[0].message!!.content)
        println("(Received in $elapsedMs ms. Tokens spent: ${explanation.usage!!.totalTokens})")
    }

    suspend fun doTranslate() {
        val start = System.currentTimeMillis()
        val translation = translate()
        val elapsedMs = System.currentTimeMillis() - start
        println()
        println(translation.choices[0].message!!.content)
        println("(Received in $elapsedMs ms. Tokens spent: ${translation.usage!!.totalTokens})")
    }

}


@OptIn(BetaOpenAI::class)
suspend fun main(args: Array<String>) {

    if (args.size != 1) {
        System.err.println("Specify the study json file as the argument")
        exitProcess(1)
    }

    val studyProcess = StudyProcess(args[0])


    var goOn = true

    while (goOn) {
        print("> ")
        val line = readlnOrNull()
        val lineAsInt = line?.toIntOrNull()
        if (line == null || line == "e" || line == "exit") {
            goOn = false
        } else if (line.startsWith("+")) {
            val newWord = line.substring(1)
            if (newWord.isBlank()) {
                println("Blank!")
            } else {
                studyProcess.add(newWord)
            }
        } else if (line.isBlank()) {
            studyProcess.nextText()
        } else if (lineAsInt != null) {
            studyProcess.toggleWord(lineAsInt)
        } else if (line == "unfocus") {
            studyProcess.unfocusAll()
        } else if (line.startsWith("? ")) {
            val what = line.substring(2)
            studyProcess.explain(what)
        } else if (line == "?") {
            studyProcess.doTranslate()
        } else {
            println("Unknown command. To add words, prefix them with +")
        }
    }

}





data class WorkingListWord(
    val number: Int,
    val word: String,
    val focused: Boolean
)