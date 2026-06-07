package com.example.closenest.features.chatbot.repository

import android.content.Context
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import org.json.JSONArray
import org.json.JSONObject

class GiftCatalogService(
    private val context: Context,
    private val assetFileName: String = GiftCatalogAssetFileName
) {
    private val products: List<GiftProduct> by lazy { loadProducts() }
    private val productsById: Map<String, GiftProduct> by lazy { products.associateBy { it.id } }

    fun buildGiftReply(
        recipientQuery: String,
        relationships: List<RelationshipProfile>
    ): GiftReply {
        val normalizedQuery = recipientQuery.trim()
        if (normalizedQuery.isBlank()) {
            return GiftReply.AskForRecipient(
                "Mình cần tên người bạn muốn tặng quà trước đã. Bạn nhập tên đúng hoặc gần đúng trong danh bạ nhé."
            )
        }

        val matchedRelationship = findRecipient(recipientQuery, relationships)
            ?: return GiftReply.RecipientNotFound(
                "Mình chưa thấy \"$normalizedQuery\" trong danh bạ. Bạn nhập lại đúng tên hoặc thêm người này vào danh bạ trước nhé."
            )

        return GiftReply.RecipientConfirmed(
            recipient = matchedRelationship,
            message = buildRecipientConfirmation(matchedRelationship)
        )
    }

    fun findRecipient(
        recipientQuery: String,
        relationships: List<RelationshipProfile>
    ): RelationshipProfile? = relationships.findBestMatch(recipientQuery)

    fun buildGiftSelectionPrompt(
        recipient: RelationshipProfile,
        requirements: String?
    ): String {
        val requirementText = requirements?.trim().takeUnless { it.isNullOrBlank() }
            ?: "Không có yêu cầu đặc biệt."
        val catalogJson = JSONArray().apply {
            products.forEach { product ->
                put(
                    JSONObject()
                        .put("id", product.id)
                        .put("name", product.name)
                        .put("price", product.price)
                        .put("category", product.category)
                        .put("relationship", JSONArray(product.relationships.toList()))
                        .put("occasion", JSONArray(product.occasions.toList()))
                        .put("tags", JSONArray(product.tags.toList()))
                        .put("short_reason", product.shortReason)
                )
            }
        }.toString(2)

        return """
            Bạn là chatbot tư vấn quà tặng trong ứng dụng CloseNest.
            Nhiệm vụ của bạn là chọn 1 hoặc 2 món quà từ danh sách có sẵn, rồi viết lời gợi ý thật tự nhiên bằng tiếng Việt.

            Thông tin người nhận:
            - Tên: ${recipient.name}
            - Quan hệ: ${recipient.tag.toVietnameseLabel()}
            - Mức độ thân thiết: ${recipient.priority.toVietnameseLabel()}
            - Sở thích: ${recipient.interests.takeIf { it.isNotEmpty() }?.joinToString() ?: "Chưa có"}
            - Ghi chú: ${recipient.notes?.takeIf { it.isNotBlank() } ?: "Chưa có"}

            Yêu cầu thêm từ người dùng:
            $requirementText

            Danh sách quà hiện có:
            $catalogJson

            Yêu cầu bắt buộc:
            - Chỉ được chọn từ danh sách quà ở trên.
            - Không được tự tạo ra sản phẩm, giá hay id mới.
            - Chọn 1 hoặc 2 món phù hợp nhất.
            - Viết lời giải thích tự nhiên, không được lặp lại metadata theo kiểu liệt kê máy móc.
            - Không nhắc đến "catalog", "dataset", "danh sách nội bộ", hay "hệ thống".
            - Không chèn link mua hàng vào phần giải thích, vì link sẽ được gửi ở tin nhắn riêng.
            - Nếu người dùng không có yêu cầu đặc biệt, hãy ưu tiên chọn quà hợp với mối quan hệ và phong cách người nhận.

            Trả về đúng JSON theo schema sau:
            {
              "intro": "một đoạn mở đầu tự nhiên, ngắn gọn",
              "items": [
                {
                  "id": "id_san_pham",
                  "reason": "lý do chọn món này, tự nhiên"
                }
              ]
            }
        """.trimIndent()
    }

    fun buildRecommendationsReplyFromAi(
        recipient: RelationshipProfile,
        requirements: String?,
        aiText: String
    ): GiftReply {
        val parsed = parseAiSelection(aiText)
            ?: return fallbackReply(
                recipient = recipient,
                requirements = requirements
            )

        val selectedProducts = parsed.items
            .mapNotNull { item ->
                productsById[item.id]?.let { product -> product to item.reason }
            }
            .distinctBy { it.first.id }
            .take(MaxGiftRecommendations)

        if (selectedProducts.isEmpty()) {
            return fallbackReply(
                recipient = recipient,
                requirements = requirements
            )
        }

        val intro = parsed.intro
            ?.takeIf { it.isNotBlank() }
            ?: buildFallbackIntro(recipient, requirements)
        val suggestionLines = selectedProducts.joinToString(separator = "\n") { (product, reason) ->
            "Mình gợi ý ${product.name} (${product.price.toVndText()}) vì ${reason.trimEnd('.', ' ')}."
        }
        val linkMessages = selectedProducts.map { (product, _) ->
            "Link mua ${product.name}: ${product.shopeeUrl}"
        }

        return GiftReply.Recommendations(
            recipient = recipient,
            summary = "$intro\n$suggestionLines",
            linkMessages = linkMessages
        )
    }

    private fun fallbackReply(
        recipient: RelationshipProfile,
        requirements: String?
    ): GiftReply {
        val fallbackProducts = products
            .filter { recipient.tag.toCatalogRelationship() in it.relationships }
            .take(MaxGiftRecommendations)

        if (fallbackProducts.isEmpty()) {
            return GiftReply.RecipientFoundButNoProduct(
                recipient = recipient,
                summary = "Mình đã xem lại thông tin của ${recipient.name}, nhưng hiện chưa chọn được món nào thật sự hợp. Bạn thử bổ sung thêm vài lựa chọn quà khác nhé."
            )
        }

        val intro = buildFallbackIntro(recipient, requirements)
        val suggestionLines = fallbackProducts.joinToString(separator = "\n") { product ->
            "Mình gợi ý ${product.name} (${product.price.toVndText()}) vì ${product.shortReason.trimEnd('.', ' ')}."
        }
        val linkMessages = fallbackProducts.map { product ->
            "Link mua ${product.name}: ${product.shopeeUrl}"
        }
        return GiftReply.Recommendations(
            recipient = recipient,
            summary = "$intro\n$suggestionLines",
            linkMessages = linkMessages
        )
    }

    private fun buildFallbackIntro(
        recipient: RelationshipProfile,
        requirements: String?
    ): String {
        val requirementPart = requirements?.trim()
            ?.takeIf { it.isNotBlank() && !it.isGenericNoRequirement() }
            ?.let { " Dựa trên yêu cầu \"$it\", mình ưu tiên những món dễ chốt và hợp hoàn cảnh hơn." }
            .orEmpty()
        val interestPart = recipient.interests
            .takeIf { it.isNotEmpty() }
            ?.let { " Mình có để ý là ${recipient.name} khá hợp với kiểu quà liên quan đến ${it.take(2).joinToString(" và ")}." }
            .orEmpty()
        return "Mình nghĩ theo hồ sơ của ${recipient.name}, mình nên chọn quà theo hướng vừa hợp người nhận vừa dễ tặng.$interestPart$requirementPart"
    }

    private fun buildRecipientConfirmation(relationship: RelationshipProfile): String {
        return buildString {
            append("Có phải bạn muốn tặng cho ${relationship.name} không? ")
            append(
                when (relationship.priority) {
                    RelationshipPriority.High -> "Nghe có vẻ đây là người khá thân với bạn. "
                    RelationshipPriority.Medium -> "Mình đã thấy đúng người bạn muốn tặng rồi. "
                    RelationshipPriority.Low -> "Mình đã tìm thấy người này trong danh bạ. "
                }
            )
            append("Bạn có yêu cầu gì đặc biệt không, ví dụ tặng dịp gì, ngân sách bao nhiêu, hoặc muốn quà theo kiểu nào?")
        }
    }

    private fun parseAiSelection(aiText: String): ParsedGiftSelection? {
        val jsonText = extractJsonObject(aiText) ?: return null
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        val items = root.optJSONArray("items")
            ?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val id = item.optString("id").trim()
                        val reason = item.optString("reason").trim()
                        if (id.isNotEmpty() && reason.isNotEmpty()) {
                            add(ParsedGiftItem(id = id, reason = reason))
                        }
                    }
                }
            }
            .orEmpty()
        if (items.isEmpty()) return null

        return ParsedGiftSelection(
            intro = root.optString("intro").trim().ifBlank { null },
            items = items
        )
    }

    private fun extractJsonObject(text: String): String? {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return null
        return text.substring(startIndex, endIndex + 1)
    }

    private fun loadProducts(): List<GiftProduct> {
        val jsonText = context.assets.open(assetFileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val jsonArray = JSONArray(jsonText)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                add(item.toGiftProduct())
            }
        }
    }
}

sealed interface GiftReply {
    data class AskForRecipient(val message: String) : GiftReply
    data class RecipientNotFound(val message: String) : GiftReply
    data class RecipientConfirmed(
        val recipient: RelationshipProfile,
        val message: String
    ) : GiftReply
    data class RecipientFoundButNoProduct(
        val recipient: RelationshipProfile,
        val summary: String
    ) : GiftReply
    data class Recommendations(
        val recipient: RelationshipProfile,
        val summary: String,
        val linkMessages: List<String>
    ) : GiftReply
}

data class GiftProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val relationships: Set<String>,
    val occasions: Set<String>,
    val tags: Set<String>,
    val shortReason: String,
    val shopeeUrl: String
)

private data class ParsedGiftSelection(
    val intro: String?,
    val items: List<ParsedGiftItem>
)

private data class ParsedGiftItem(
    val id: String,
    val reason: String
)

private fun JSONObject.toGiftProduct(): GiftProduct {
    return GiftProduct(
        id = optString("id"),
        name = optString("name"),
        category = optString("category").normalizeForMatch(),
        price = optInt("price"),
        relationships = optStringArray("relationship").map { it.normalizeForMatch() }.toSet(),
        occasions = optStringArray("occasion").map { it.normalizeForMatch() }.toSet(),
        tags = optStringArray("tags").map { it.normalizeForMatch() }.toSet(),
        shortReason = optString("short_reason"),
        shopeeUrl = optString("shopee_url")
    )
}

private fun JSONObject.optStringArray(fieldName: String): List<String> {
    val array = optJSONArray(fieldName) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotEmpty()) add(value)
        }
    }
}

private fun List<RelationshipProfile>.findBestMatch(query: String): RelationshipProfile? {
    val normalizedQuery = query.normalizeForMatch()
    val queryTokens = normalizedQuery.normalizedTokens().toSet()
    return this
        .map { relationship ->
            relationship to relationship.name.matchScore(
                normalizedQuery = normalizedQuery,
                queryTokens = queryTokens
            )
        }
        .filter { (_, score) -> score > 0 }
        .sortedWith(
            compareByDescending<Pair<RelationshipProfile, Int>> { it.second }
                .thenByDescending { it.first.priority.value }
        )
        .firstOrNull()
        ?.first
}

private fun String.matchScore(
    normalizedQuery: String,
    queryTokens: Set<String>
): Int {
    val normalizedName = normalizeForMatch()
    val nameTokens = normalizedName.normalizedTokens().toSet()
    val overlappingTokens = nameTokens.intersect(queryTokens)
    val partialTokenMatches = queryTokens.count { queryToken ->
        queryToken.length >= MinPartialTokenLength &&
            nameTokens.any { nameToken ->
                nameToken.contains(queryToken) || queryToken.contains(nameToken)
            }
    }

    return when {
        normalizedName == normalizedQuery -> 100
        normalizedQuery.length >= MinPartialTokenLength &&
            normalizedName.startsWith(normalizedQuery) -> 90
        normalizedQuery.length >= MinPartialTokenLength &&
            normalizedName.contains(normalizedQuery) -> 85
        normalizedName in queryTokens -> 82
        normalizedQuery.contains(normalizedName) -> 80
        overlappingTokens.size >= 2 -> 70 + overlappingTokens.size
        overlappingTokens.size == 1 -> 62
        partialTokenMatches > 0 -> 50 + partialTokenMatches
        normalizedName.startsWith(normalizedQuery) -> 80
        normalizedName.contains(normalizedQuery) -> 60
        else -> 0
    }
}

private fun RelationshipTag.toCatalogRelationship(): String {
    return when (this) {
        RelationshipTag.Partner -> CatalogRelationshipPartner
        RelationshipTag.Family -> CatalogRelationshipParent
        RelationshipTag.Coworker -> CatalogRelationshipCoworker
        RelationshipTag.Friend,
        RelationshipTag.CloseFriend,
        RelationshipTag.Classmate,
        RelationshipTag.Other -> CatalogRelationshipFriend
    }
}

private fun RelationshipTag.toVietnameseLabel(): String {
    return when (this) {
        RelationshipTag.Family -> "gia đình"
        RelationshipTag.Friend -> "bạn bè"
        RelationshipTag.CloseFriend -> "bạn thân"
        RelationshipTag.Classmate -> "bạn học"
        RelationshipTag.Coworker -> "đồng nghiệp"
        RelationshipTag.Partner -> "người yêu"
        RelationshipTag.Other -> "khác"
    }
}

private fun RelationshipPriority.toVietnameseLabel(): String {
    return when (this) {
        RelationshipPriority.Low -> "thông thường"
        RelationshipPriority.Medium -> "thân thiết"
        RelationshipPriority.High -> "rất thân thiết"
    }
}

private fun Int.toVndText(): String = "${this / 1_000}k"

private fun String.normalizedTokens(): List<String> {
    return normalizeForMatch()
        .split(" ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun String.isGenericNoRequirement(): Boolean {
    val normalized = normalizeForMatch()
    return normalized in setOf(
        "khong",
        "khong co",
        "khong co yeu cau",
        "khong yeu cau gi",
        "khong co gi dac biet"
    )
}

private fun String.normalizeForMatch(): String {
    return java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD)
        .replace("đ", "d")
        .replace("Đ", "D")
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private const val GiftCatalogAssetFileName = "gift_dataset_18_samples.json"
private const val MaxGiftRecommendations = 2
private const val CatalogRelationshipPartner = "girlfriend"
private const val CatalogRelationshipParent = "parent"
private const val CatalogRelationshipFriend = "friend"
private const val CatalogRelationshipCoworker = "coworker"
private const val MinPartialTokenLength = 2
