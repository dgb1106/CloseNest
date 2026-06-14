package com.example.closenest.features.chatbot.repository

import com.example.closenest.BuildConfig
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface ChatbotAiService {
    val modelName: String

    suspend fun generateReply(
        mode: ChatMode,
        messages: List<ChatMessage>
    ): ChatbotAiReply

    suspend fun generateReplyFromPrompt(prompt: String): ChatbotAiReply
}

data class ChatbotAiReply(
    val text: String,
    val modelName: String
)

class GeminiChatbotAiService(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    override val modelName: String = GeminiModelName,
    private val fallbackModelNames: List<String> = GeminiFallbackModelNames
) : ChatbotAiService {
    override suspend fun generateReply(
        mode: ChatMode,
        messages: List<ChatMessage>
    ): ChatbotAiReply = withContext(Dispatchers.IO) {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            throw GeminiApiException(
                statusCode = 0,
                apiMessage = "Missing GEMINI_API_KEY in local.properties."
            )
        }

        val requestBody = buildGeminiRequestBody(buildPrompt(mode, messages)).toString()
        requestReply(requestBody)
    }

    override suspend fun generateReplyFromPrompt(prompt: String): ChatbotAiReply =
        withContext(Dispatchers.IO) {
            val trimmedApiKey = apiKey.trim()
            if (trimmedApiKey.isBlank()) {
                throw GeminiApiException(
                    statusCode = 0,
                    apiMessage = "Missing GEMINI_API_KEY in local.properties."
                )
            }

            val requestBody = buildGeminiRequestBody(prompt).toString()
            requestReply(requestBody)
        }

    private fun requestReply(requestBody: String): ChatbotAiReply {
        val candidateModels = (listOf(modelName) + fallbackModelNames).distinct()
        var lastDemandException: GeminiApiException? = null

        candidateModels.forEachIndexed { index, candidateModel ->
            try {
                return ChatbotAiReply(
                    text = requestGeminiReply(
                        modelName = candidateModel,
                        apiKey = apiKey.trim(),
                        requestBody = requestBody
                    ),
                    modelName = candidateModel
                )
            } catch (exception: GeminiApiException) {
                val canFallback = index < candidateModels.lastIndex &&
                    exception.isModelDemandError()
                if (!canFallback) {
                    throw exception
                }
                lastDemandException = exception
            }
        }

        throw lastDemandException ?: GeminiApiException(
            statusCode = 0,
            apiMessage = "No Gemini model was available."
        )
    }

    private fun requestGeminiReply(
        modelName: String,
        apiKey: String,
        requestBody: String
    ): String {
        val connection = (URL("$GeminiEndpointBase/$modelName:generateContent")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = GeminiTimeoutMillis
            readTimeout = GeminiTimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        return try {
            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseBody = connection.readBody(responseCode)

            if (responseCode !in 200..299) {
                throw GeminiApiException(
                    statusCode = responseCode,
                    apiMessage = responseBody.toGeminiErrorMessage()
                )
            }

            responseBody.toGeminiReply()
                ?: throw GeminiApiException(
                    statusCode = responseCode,
                    apiMessage = "Gemini returned an empty response."
                )
        } finally {
            connection.disconnect()
        }
    }
}

class GeminiApiException(
    val statusCode: Int,
    val apiMessage: String
) : RuntimeException("Gemini API error $statusCode: $apiMessage")

private fun GeminiApiException.isModelDemandError(): Boolean {
    val message = apiMessage.lowercase()
    val isKeyOrBillingError = message.contains("api key") ||
        message.contains("billing") ||
        message.contains("prepayment") ||
        message.contains("depleted")
    if (isKeyOrBillingError) return false

    return statusCode == 500 ||
        statusCode == 503 ||
        message.contains("overload") ||
        message.contains("capacity") ||
        message.contains("unavailable") ||
        message.contains("try again later") ||
        message.contains("high demand") ||
        (statusCode == 429 && message.contains("rate limit"))
}

object ChatbotAiServiceProvider {
    val service: ChatbotAiService by lazy {
        GeminiChatbotAiService()
    }
}

private fun buildPrompt(
    mode: ChatMode,
    messages: List<ChatMessage>
): String {
    val recentMessages = messages.takeLast(MaxPromptMessages)
    val transcript = recentMessages.joinToString(separator = "\n") { message ->
        val speaker = when (message.role) {
            ChatRole.User -> "Nguoi dung"
            ChatRole.Assistant -> "CloseNest"
        }
        "$speaker: ${message.text}"
    }

    return """
        Ban la chatbot dong hanh trong ung dung CloseNest. Hay tra loi bang tieng Viet tu nhien, am ap, ro rang va tron y.
        Dung tra loi cut lun. Neu cau hoi can giai thich, hay viet thanh 2-5 doan ngan hoac mot danh sach ngan de nguoi dung co du thong tin.
        Khong chan doan tam ly, khong thay the bac si, luat su hay chuyen gia. Khong dua huong dan gay hai.
        Neu nguoi dung co y dinh tu hai, hay phan hoi binh tinh, khuyen ho lien he nguoi than tin cay hoac dich vu khan cap tai dia phuong ngay lap tuc.
        Neu nguoi dung noi muon lam hai nguoi khac, hay giup ho tam dung, roi khoi tinh huong cang thang va tim su tro giup an toan.

        Che do hien tai:
        ${mode.instructions()}

        Lich su gan day:
        $transcript

        Hay tra loi tin nhan cuoi cung cua nguoi dung mot cach day du, khong ket thuc dot ngot. Neu can them thong tin, chi hoi 1-2 cau that can thiet.
    """.trimIndent()
}

private fun ChatMode.instructions(): String {
    return when (this) {
        ChatMode.General -> """
            Nguoi dung dang bat dau mot cuoc tro chuyen mo. Hay bam sat tin nhan cua ho, tra loi huu ich va am ap.
            Neu ho can y tuong, hay dua goi y cu the. Neu ho can duoc lang nghe, hay phan hoi cham rai va co su dong cam.
        """.trimIndent()
        ChatMode.Vent -> """
            Nguoi dung muon xa cam xuc. Hay lang nghe, goi ten cam xuc, cong nhan cam giac cua ho, va hoi mot cau nhe de ho noi tiep.
            Tra loi nhu mot nguoi ban binh tinh. Khong day doi, khong phan xet, khong bien cau tra loi thanh bai giang. Co the them mot goi y nho neu phu hop.
        """.trimIndent()
        ChatMode.GiftAdvice -> """
            Nguoi dung can tu van qua tang. Hay hoi ve nguoi nhan, dip tang, ngan sach, so thich, moi quan he va thoi gian neu con thieu.
            Khi du thong tin, de xuat 3-5 mon qua kem ly do, muc gia uoc luong va cach ca nhan hoa.
        """.trimIndent()
    }
}

const val GeminiModelName = "gemini-2.5-flash"
val GeminiFallbackModelNames = listOf("gemini-2.5-flash-lite")

private fun buildGeminiRequestBody(prompt: String): JSONObject {
    return JSONObject()
        .put(
            "contents",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
            )
        )
        .put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0.7)
                .put("topP", 0.9)
                .put("maxOutputTokens", 2048)
        )
        .put(
            "safetySettings",
            JSONArray()
                .put(
                    JSONObject()
                        .put("category", "HARM_CATEGORY_HARASSMENT")
                        .put("threshold", "BLOCK_ONLY_HIGH")
                )
                .put(
                    JSONObject()
                        .put("category", "HARM_CATEGORY_HATE_SPEECH")
                        .put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                )
                .put(
                    JSONObject()
                        .put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                        .put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                )
                .put(
                    JSONObject()
                        .put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                        .put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                )
        )
}

private fun HttpURLConnection.readBody(responseCode: Int): String {
    val stream = if (responseCode in 200..299) {
        inputStream
    } else {
        errorStream
    } ?: return ""

    return stream.bufferedReader(Charsets.UTF_8).use { reader ->
        reader.readText()
    }
}

private fun String.toGeminiReply(): String? {
    val root = JSONObject(this)
    val promptBlockReason = root.optJSONObject("promptFeedback")
        ?.optString("blockReason")
        ?.takeIf { value -> value.isNotBlank() }
    if (promptBlockReason != null) {
        throw GeminiApiException(
            statusCode = 200,
            apiMessage = "Prompt was blocked: $promptBlockReason"
        )
    }

    val candidate = root.optJSONArray("candidates")
        ?.optJSONObject(0)
        ?: return null
    val finishReason = candidate.optString("finishReason")
    if (finishReason == "SAFETY" || finishReason == "RECITATION") {
        throw GeminiApiException(
            statusCode = 200,
            apiMessage = "Content generation stopped: $finishReason"
        )
    }

    val parts = candidate.optJSONObject("content")
        ?.optJSONArray("parts")
        ?: return null
    return buildString {
        for (index in 0 until parts.length()) {
            val text = parts.optJSONObject(index)
                ?.optString("text")
                ?.takeIf { value -> value.isNotBlank() }
            if (text != null) {
                if (isNotBlank()) append("\n")
                append(text)
            }
        }
    }.trim().takeIf { text -> text.isNotBlank() }
}

private fun String.toGeminiErrorMessage(): String {
    return runCatching {
        JSONObject(this)
            .optJSONObject("error")
            ?.optString("message")
            ?.takeIf { value -> value.isNotBlank() }
    }.getOrNull()
        ?: ifBlank { "Unknown Gemini API error." }
}

private const val GeminiEndpointBase =
    "https://generativelanguage.googleapis.com/v1beta/models"
private const val GeminiTimeoutMillis = 45_000
private const val MaxPromptMessages = 16
