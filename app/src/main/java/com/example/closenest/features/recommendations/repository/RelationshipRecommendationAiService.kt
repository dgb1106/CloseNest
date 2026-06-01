package com.example.closenest.features.recommendations.repository

import com.example.closenest.BuildConfig
import com.example.closenest.features.recommendations.model.RecommendationAction
import com.example.closenest.features.recommendations.model.RelationshipActivityContext
import com.example.closenest.features.recommendations.model.RelationshipComputedMetrics
import com.example.closenest.features.recommendations.model.RelationshipEvaluationContext
import com.example.closenest.features.recommendations.model.RelationshipEvaluationProfile
import com.example.closenest.features.recommendations.model.RelationshipImportantSignals
import com.example.closenest.features.recommendations.model.RelationshipRecommendationDraft
import com.example.closenest.features.recommendations.model.RelationshipReflectionContext
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface RelationshipRecommendationAiService {
    val modelName: String
    val promptVersion: String

    suspend fun evaluate(context: RelationshipEvaluationContext): RelationshipRecommendationAiReply
}

data class RelationshipRecommendationAiReply(
    val draft: RelationshipRecommendationDraft,
    val modelName: String,
    val rawJson: String
)

class GeminiRelationshipRecommendationAiService(
    private val apiKey: String = BuildConfig.GEMINI_API_SCORE_KEY,
    override val modelName: String = RecommendationGeminiModelName,
    private val fallbackModelNames: List<String> = RecommendationGeminiFallbackModelNames,
    override val promptVersion: String = RecommendationPromptVersion
) : RelationshipRecommendationAiService {
    override suspend fun evaluate(
        context: RelationshipEvaluationContext
    ): RelationshipRecommendationAiReply = withContext(Dispatchers.IO) {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            throw RecommendationGeminiApiException(
                statusCode = 0,
                apiMessage = "Missing GEMINI_API_SCORE_KEY in local.properties."
            )
        }

        val requestBody = buildGeminiRequestBody(buildPrompt(context)).toString()
        val candidateModels = (listOf(modelName) + fallbackModelNames).distinct()
        var lastDemandException: RecommendationGeminiApiException? = null

        candidateModels.forEachIndexed { index, candidateModel ->
            try {
                val responseText = requestGemini(
                    modelName = candidateModel,
                    apiKey = trimmedApiKey,
                    requestBody = requestBody
                )
                val rawJson = responseText.extractJsonObject()
                return@withContext RelationshipRecommendationAiReply(
                    draft = rawJson.toRecommendationDraft(context),
                    modelName = candidateModel,
                    rawJson = rawJson
                )
            } catch (exception: RecommendationGeminiApiException) {
                val canFallback = index < candidateModels.lastIndex &&
                    exception.isModelDemandError()
                if (!canFallback) {
                    throw exception
                }
                lastDemandException = exception
            }
        }

        throw lastDemandException ?: RecommendationGeminiApiException(
            statusCode = 0,
            apiMessage = "No Gemini model was available for recommendation scoring."
        )
    }

    private fun requestGemini(
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
                throw RecommendationGeminiApiException(
                    statusCode = responseCode,
                    apiMessage = responseBody.toGeminiErrorMessage()
                )
            }

            responseBody.toGeminiText()
                ?: throw RecommendationGeminiApiException(
                    statusCode = responseCode,
                    apiMessage = "Gemini returned an empty recommendation response."
                )
        } finally {
            connection.disconnect()
        }
    }
}

class RecommendationGeminiApiException(
    val statusCode: Int,
    val apiMessage: String
) : RuntimeException("Gemini recommendation API error $statusCode: $apiMessage")

object RelationshipRecommendationAiServiceProvider {
    val service: RelationshipRecommendationAiService by lazy {
        GeminiRelationshipRecommendationAiService()
    }
}

private fun buildPrompt(context: RelationshipEvaluationContext): String {
    return """
        Bạn là Relationship Recommendation Engine cho ứng dụng CloseNest.
        Nhiệm vụ: dựa trên dữ liệu đã được app tổng hợp, đánh giá ngắn gọn trạng thái quan hệ và đề xuất hành động phù hợp.

        Nguyên tắc:
        - Chỉ dùng dữ liệu trong JSON input, không bịa sự kiện mới.
        - Không dùng reciprocity vì app cá nhân không biết ai chủ động.
        - Không coi mood/reflection là sentiment của mối quan hệ nếu không có nội dung rõ ràng; chỉ dùng như context phụ.
        - Giọng văn tiếng Việt tự nhiên, riêng tư, nhẹ nhàng.
        - suggestedText phải ngắn, có thể gửi được ngay, tránh quá thân mật nếu tag không phải Partner/CloseFriend/Family.
        - Luôn trả về đúng JSON object, không markdown, không giải thích ngoài JSON.

        JSON input:
        ${context.toJson().toString(2)}
    """.trimIndent()
}

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
                .put("temperature", 0.35)
                .put("topP", 0.85)
                .put("maxOutputTokens", 2048)
                .put("responseMimeType", "application/json")
                .put("responseSchema", buildResponseSchema())
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

private fun buildResponseSchema(): JSONObject {
    val actionSchema = JSONObject()
        .put("type", "OBJECT")
        .put(
            "properties",
            JSONObject()
                .put("type", JSONObject().put("type", "STRING"))
                .put("priority", JSONObject().put("type", "STRING"))
                .put("title", JSONObject().put("type", "STRING"))
                .put("suggestedText", JSONObject().put("type", "STRING"))
        )
        .put("required", JSONArray(listOf("type", "priority", "title")))

    return JSONObject()
        .put("type", "OBJECT")
        .put(
            "properties",
            JSONObject()
                .put("relationshipStatus", JSONObject().put("type", "STRING"))
                .put("summary", JSONObject().put("type", "STRING"))
                .put(
                    "reasons",
                    JSONObject()
                        .put("type", "ARRAY")
                        .put("items", JSONObject().put("type", "STRING"))
                )
                .put(
                    "suggestedActions",
                    JSONObject()
                        .put("type", "ARRAY")
                        .put("items", actionSchema)
                )
                .put("primaryActionType", JSONObject().put("type", "STRING"))
                .put("primaryActionLabel", JSONObject().put("type", "STRING"))
                .put("notificationTitle", JSONObject().put("type", "STRING"))
                .put("notificationDescription", JSONObject().put("type", "STRING"))
        )
        .put(
            "required",
            JSONArray(
                listOf(
                    "relationshipStatus",
                    "summary",
                    "reasons",
                    "suggestedActions",
                    "primaryActionType",
                    "primaryActionLabel",
                    "notificationTitle",
                    "notificationDescription"
                )
            )
        )
}

private fun RelationshipEvaluationContext.toJson(): JSONObject {
    return JSONObject()
        .put("currentDate", currentDateIso)
        .put("relationship", relationship.toJson())
        .put("computedMetrics", computedMetrics.toJson())
        .put("recentActivities", recentActivities.toJsonArray { activity -> activity.toJson() })
        .put("importantSignals", importantSignals.toJson())
        .put("reflectionContext", reflectionContext.toJsonArray { reflection -> reflection.toJson() })
}

private fun RelationshipEvaluationProfile.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("tag", tag)
        .put("priority", priority)
        .put("birthdayIso", birthdayIso)
        .put("interests", JSONArray(interests))
        .put("notes", notes)
        .put("createdAtMillis", createdAtMillis)
}

private fun RelationshipComputedMetrics.toJson(): JSONObject {
    return JSONObject()
        .put("bondScore", bondScore)
        .put("attentionScore", attentionScore)
        .put("bondLabel", bondLabel)
        .put("attentionLabel", attentionLabel)
        .put("hasLoggedTouch", hasLoggedTouch)
        .put("lastTouchAtMillis", lastTouchAtMillis)
        .put("lastTouchType", lastTouchType)
        .put("daysSinceLastTouch", daysSinceLastTouch)
        .put("expectedTouchIntervalDays", expectedTouchIntervalDays)
        .put("overdueDays", overdueDays)
        .put("touchCountLast7Days", touchCountLast7Days)
        .put("touchCountLast30Days", touchCountLast30Days)
        .put("touchCountLast90Days", touchCountLast90Days)
        .put("priorityScore", priorityScore)
        .put("recencyScore", recencyScore)
        .put("frequencyScore", frequencyScore)
        .put("specialDateScore", specialDateScore)
        .put("profileRichnessScore", profileRichnessScore)
}

private fun RelationshipActivityContext.toJson(): JSONObject {
    return JSONObject()
        .put("type", type)
        .put("title", title)
        .put("note", note)
        .put("location", location)
        .put("createdAtMillis", createdAtMillis)
}

private fun RelationshipImportantSignals.toJson(): JSONObject {
    return JSONObject()
        .put("birthdayInDays", birthdayInDays)
        .put("hasBirthdaySoon", hasBirthdaySoon)
        .put("memoryAnniversaryInDays", memoryAnniversaryInDays)
        .put("hasMemoryAnniversarySoon", hasMemoryAnniversarySoon)
        .put("upcomingAppointmentInDays", upcomingAppointmentInDays)
        .put("hasUpcomingAppointment", hasUpcomingAppointment)
}

private fun RelationshipReflectionContext.toJson(): JSONObject {
    return JSONObject()
        .put("mood", mood)
        .put("feelings", JSONArray(feelings))
        .put("sources", JSONArray(sources))
        .put("createdAtMillis", createdAtMillis)
}

private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
    val array = JSONArray()
    forEach { item -> array.put(mapper(item)) }
    return array
}

private fun String.toRecommendationDraft(
    context: RelationshipEvaluationContext
): RelationshipRecommendationDraft {
    val root = JSONObject(this)
    val actions = root.optJSONArray("suggestedActions")
        ?.toRecommendationActions()
        .orEmpty()
        .ifEmpty { listOf(context.defaultAction()) }

    val primaryActionType = root.optNonBlankString("primaryActionType")
        ?: actions.first().type
    val primaryActionLabel = root.optNonBlankString("primaryActionLabel")
        ?: primaryActionType.toActionLabel()

    return RelationshipRecommendationDraft(
        relationshipStatus = root.optNonBlankString("relationshipStatus")
            ?: context.computedMetrics.attentionLabel,
        summary = root.optNonBlankString("summary")
            ?: context.defaultSummary(),
        reasons = root.optJSONArray("reasons")
            ?.toStringList()
            .orEmpty()
            .ifEmpty { context.defaultReasons() },
        suggestedActions = actions,
        primaryActionType = primaryActionType,
        primaryActionLabel = primaryActionLabel,
        notificationTitle = root.optNonBlankString("notificationTitle")
            ?: "Gợi ý cho ${context.relationship.name}",
        notificationDescription = root.optNonBlankString("notificationDescription")
            ?: context.defaultSummary()
    )
}

private fun JSONArray.toRecommendationActions(): List<RecommendationAction> {
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val type = item.optNonBlankString("type") ?: continue
            val title = item.optNonBlankString("title") ?: continue
            add(
                RecommendationAction(
                    type = type,
                    priority = item.optNonBlankString("priority") ?: "MEDIUM",
                    title = title,
                    suggestedText = item.optNonBlankString("suggestedText")
                )
            )
        }
    }
}

private fun JSONArray.toStringList(): List<String> {
    return buildList {
        for (index in 0 until length()) {
            optString(index)
                .takeIf { value -> value.isNotBlank() }
                ?.let(::add)
        }
    }
}

private fun RelationshipEvaluationContext.defaultAction(): RecommendationAction {
    return when {
        importantSignals.hasBirthdaySoon -> RecommendationAction(
            type = "SEND_GIFT",
            priority = "HIGH",
            title = "Chuẩn bị lời chúc",
            suggestedText = "Sắp đến sinh nhật bạn rồi, mình chúc trước một chút nhé."
        )
        importantSignals.hasMemoryAnniversarySoon -> RecommendationAction(
            type = "VIEW_MEMORY",
            priority = "MEDIUM",
            title = "Nhắc lại kỷ niệm",
            suggestedText = "Tự nhiên mình nhớ lại kỷ niệm hôm đó."
        )
        else -> RecommendationAction(
            type = "SEND_MESSAGE",
            priority = "HIGH",
            title = "Nhắn tin hỏi thăm",
            suggestedText = "Dạo này bạn ổn không?"
        )
    }
}

private fun RelationshipEvaluationContext.defaultSummary(): String {
    val days = computedMetrics.daysSinceLastTouch
    return if (days == null) {
        "Bạn chưa có ghi nhận tương tác nào với ${relationship.name}."
    } else {
        "Bạn đã $days ngày chưa có ghi nhận tương tác mới với ${relationship.name}."
    }
}

private fun RelationshipEvaluationContext.defaultReasons(): List<String> {
    return buildList {
        add("Mức ưu tiên: ${relationship.priority}.")
        computedMetrics.daysSinceLastTouch?.let { days ->
            add("Lần ghi nhận gần nhất cách đây $days ngày.")
        } ?: add("Chưa có ghi nhận tương tác nào.")
        add("Ngưỡng gợi ý hiện tại là ${computedMetrics.expectedTouchIntervalDays} ngày.")
    }
}

private fun String.toActionLabel(): String {
    return when (this) {
        "SEND_MESSAGE" -> "Nhắn tin"
        "CALL" -> "Gọi điện"
        "MEETUP", "DATE" -> "Hẹn gặp"
        "SEND_GIFT" -> "Chuẩn bị quà"
        "VIEW_MEMORY" -> "Xem kỷ niệm"
        "LOG_MEMORY" -> "Ghi lại"
        else -> "Xem gợi ý"
    }
}

private fun JSONObject.optNonBlankString(name: String): String? {
    return optString(name)
        .takeIf { value -> value.isNotBlank() && value != "null" }
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

private fun String.toGeminiText(): String? {
    val root = JSONObject(this)
    val promptBlockReason = root.optJSONObject("promptFeedback")
        ?.optString("blockReason")
        ?.takeIf { value -> value.isNotBlank() }
    if (promptBlockReason != null) {
        throw RecommendationGeminiApiException(
            statusCode = 200,
            apiMessage = "Prompt was blocked: $promptBlockReason"
        )
    }

    val candidate = root.optJSONArray("candidates")
        ?.optJSONObject(0)
        ?: return null
    val finishReason = candidate.optString("finishReason")
    if (finishReason == "SAFETY" || finishReason == "RECITATION") {
        throw RecommendationGeminiApiException(
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

private fun String.extractJsonObject(): String {
    val trimmed = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    if (start < 0 || end < start) {
        throw RecommendationGeminiApiException(
            statusCode = 200,
            apiMessage = "Gemini did not return a JSON object."
        )
    }
    return trimmed.substring(start, end + 1)
}

private fun RecommendationGeminiApiException.isModelDemandError(): Boolean {
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

private const val RecommendationGeminiModelName = "gemini-2.5-flash-lite"
private val RecommendationGeminiFallbackModelNames = emptyList<String>()
private const val RecommendationPromptVersion = "relationship_recommendation_v1"
private const val GeminiEndpointBase =
    "https://generativelanguage.googleapis.com/v1beta/models"
private const val GeminiTimeoutMillis = 45_000
