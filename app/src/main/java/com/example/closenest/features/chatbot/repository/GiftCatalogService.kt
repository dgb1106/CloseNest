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
    private val products: List<GiftProduct> by lazy {
        loadProducts()
    }

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
    ): RelationshipProfile? {
        return relationships.findBestMatch(recipientQuery)
    }

    fun buildRecommendationsReply(
        recipient: RelationshipProfile,
        requirements: String?
    ): GiftReply {
        val recommendations = recommendProducts(
            relationship = recipient,
            requirements = parseRequirements(requirements)
        )
        if (recommendations.isEmpty()) {
            return GiftReply.RecipientFoundButNoProduct(
                recipient = recipient,
                summary = "Mình đã xem lại thông tin của ${recipient.name}, nhưng hiện chưa chọn được món nào thật sự hợp với yêu cầu này. Bạn thử nới ngân sách hoặc thêm vài lựa chọn quà khác nhé."
            )
        }

        val summary = buildSummary(
            relationship = recipient,
            recommendations = recommendations,
            requirements = requirements
        )
        val linkMessages = recommendations.map { product ->
            "Link mua ${product.name}: ${product.shopeeUrl}"
        }
        return GiftReply.Recommendations(
            recipient = recipient,
            summary = summary,
            linkMessages = linkMessages
        )
    }

    private fun recommendProducts(
        relationship: RelationshipProfile,
        requirements: GiftRequirements
    ): List<GiftProduct> {
        val mappedRelationship = relationship.tag.toCatalogRelationship()
        val preferenceTokens = (relationship.interests + relationship.notes.orEmpty())
            .flatMap { text -> text.normalizedTokens() }
            .toSet()

        return products
            .map { product ->
                product to scoreProduct(
                    product = product,
                    relationship = relationship,
                    mappedRelationship = mappedRelationship,
                    preferenceTokens = preferenceTokens,
                    requirements = requirements
                )
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<GiftProduct, Int>> { it.second }
                    .thenBy { it.first.price }
            )
            .map { it.first }
            .take(MaxGiftRecommendations)
    }

    private fun scoreProduct(
        product: GiftProduct,
        relationship: RelationshipProfile,
        mappedRelationship: String,
        preferenceTokens: Set<String>,
        requirements: GiftRequirements
    ): Int {
        var score = 0
        if (mappedRelationship in product.relationships) score += 6
        score += when (relationship.priority) {
            RelationshipPriority.High -> 2
            RelationshipPriority.Medium -> 1
            RelationshipPriority.Low -> 0
        }
        if (product.tags.any(preferenceTokens::contains)) score += 4
        if (product.category in preferenceTokens) score += 3
        if (product.shortReason.normalizedTokens().any(preferenceTokens::contains)) score += 2
        if (mappedRelationship == CatalogRelationshipPartner && product.tags.any { it in RomanticTags }) {
            score += 2
        }
        if (mappedRelationship == CatalogRelationshipParent && product.tags.any { it in ParentFriendlyTags }) {
            score += 2
        }
        if (requirements.maxBudget != null) {
            score += when {
                product.price <= requirements.maxBudget -> 5
                product.price <= requirements.maxBudget + 150_000 -> 1
                else -> -8
            }
        }
        if (requirements.occasionTokens.isNotEmpty() &&
            product.occasions.any { it in requirements.occasionTokens }
        ) {
            score += 4
        }
        if (requirements.preferenceTokens.isNotEmpty()) {
            if (product.tags.any { it in requirements.preferenceTokens }) score += 3
            if (product.category in requirements.preferenceTokens) score += 2
        }
        return score
    }

    private fun buildSummary(
        relationship: RelationshipProfile,
        recommendations: List<GiftProduct>,
        requirements: String?
    ): String {
        val intro = buildIntro(relationship, requirements)

        val suggestions = recommendations.joinToString(separator = "\n") { product ->
            buildSuggestionLine(relationship, product)
        }

        return "$intro\n$suggestions"
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

    private fun buildRecipientConfirmation(relationship: RelationshipProfile): String {
        val tone = when (relationship.priority) {
            RelationshipPriority.High -> "Nghe có vẻ đây là người khá thân với bạn."
            RelationshipPriority.Medium -> "Mình đã thấy đúng người bạn muốn tặng rồi."
            RelationshipPriority.Low -> "Mình đã tìm thấy người này trong danh bạ."
        }
        return buildString {
            append("Có phải bạn muốn tặng cho ${relationship.name} không? ")
            append(tone)
            append("Bạn có yêu cầu gì đặc biệt không, ví dụ tặng dịp gì, ngân sách bao nhiêu, hoặc muốn món quà theo kiểu nào?")
        }
    }

    private fun buildIntro(
        relationship: RelationshipProfile,
        requirements: String?
    ): String {
        val closeness = when (relationship.priority) {
            RelationshipPriority.High -> "Bạn này khá thân với bạn"
            RelationshipPriority.Medium -> "Nghe có vẻ đây là một người khá quan trọng với bạn"
            RelationshipPriority.Low -> "Mình đã xem qua hồ sơ của ${relationship.name}"
        }

        val interestsPart = relationship.interests
            .takeIf { it.isNotEmpty() }
            ?.let { interests ->
                " và có vẻ ${relationship.name} khá thích ${interests.take(2).joinToString(" và ")}"
            }
            .orEmpty()

        val notesPart = relationship.notes
            ?.takeIf { it.isNotBlank() }
            ?.let { note ->
                when {
                    note.length <= 60 -> ", thêm nữa bạn còn ghi chú là $note"
                    else -> ""
                }
            }
            .orEmpty()

        val requirementPart = requirements
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.isGenericNoRequirement() }
            ?.let { " Dựa trên yêu cầu \"$it\", mình nghiêng về những món dễ chốt và hợp bối cảnh này hơn." }
            .orEmpty()

        return "$closeness$interestsPart$notesPart, nên mình chọn theo hướng vừa hợp tính cách vừa dễ tặng.$requirementPart"
    }

    private fun buildSuggestionLine(
        relationship: RelationshipProfile,
        product: GiftProduct
    ): String {
        val personalizedReason = when {
            relationship.interests.isNotEmpty() && productMatchesInterest(product, relationship.interests) ->
                "món này khá khớp với những gì ${relationship.name} thích"
            relationship.tag == RelationshipTag.Partner ->
                "món này tạo cảm giác tinh tế và đủ riêng tư để tặng người yêu"
            relationship.tag == RelationshipTag.Family ->
                "món này thiết thực, dễ dùng và hợp để tặng người thân trong gia đình"
            relationship.tag == RelationshipTag.Coworker ->
                "món này lịch sự, an toàn và hợp để tặng đồng nghiệp"
            relationship.priority == RelationshipPriority.High ->
                "món này đủ gần gũi để thể hiện bạn có để ý đến người nhận"
            else ->
                product.shortReason.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val bridge = when (product.category) {
            "food", "coffee", "tea" -> "dễ tặng mà không quá rủi ro"
            "health", "wellness" -> "vừa có cảm giác quan tâm vừa dùng được lâu"
            "decor", "cute", "personalized" -> "nhìn vào là có cảm giác quà được chọn kỹ"
            else -> "khá cân bằng giữa cảm xúc và tính thực tế"
        }

        return "Mình gợi ý ${product.name} (${product.price.toVndText()}) vì $personalizedReason, lại $bridge."
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

data class GiftRequirements(
    val maxBudget: Int? = null,
    val occasionTokens: Set<String> = emptySet(),
    val preferenceTokens: Set<String> = emptySet()
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

private fun productMatchesInterest(
    product: GiftProduct,
    interests: List<String>
): Boolean {
    val interestTokens = interests.flatMap { it.normalizedTokens() }.toSet()
    return product.tags.any { it in interestTokens } ||
        product.category in interestTokens ||
        product.shortReason.normalizedTokens().any { it in interestTokens }
}

private fun parseRequirements(input: String?): GiftRequirements {
    val text = input.orEmpty()
    val normalized = text.normalizeForMatch()
    val tokens = normalized.normalizedTokens().toSet()
    val budget = extractBudget(text)
    val occasionTokens = OccasionKeywordMap
        .filter { (keyword, _) -> keyword in normalized }
        .values
        .toSet()

    return GiftRequirements(
        maxBudget = budget,
        occasionTokens = occasionTokens,
        preferenceTokens = tokens - StopRequirementTokens
    )
}

private fun extractBudget(input: String): Int? {
    val normalized = input.lowercase()
    val match = BudgetRegex.find(normalized) ?: return null
    val value = match.groupValues[1].toIntOrNull() ?: return null
    val unit = match.groupValues[2]
    return when {
        unit.contains("tr") || unit.contains("triệu") -> value * 1_000_000
        unit.contains("k") || unit.contains("ngh") || unit.contains("ngàn") -> value * 1_000
        else -> value
    }
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
        RelationshipTag.Mentor,
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
        RelationshipTag.Mentor -> "người hướng dẫn"
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

private val RomanticTags = setOf("romantic", "cute", "flower", "memory")
private val ParentFriendlyTags = setOf("health", "practical", "traditional", "healthy", "warm")
private val BudgetRegex = Regex("""(\d+)\s*(k|ngh|ngàn|tr|triệu)?""", RegexOption.IGNORE_CASE)
private val OccasionKeywordMap = mapOf(
    "sinh nhat" to "birthday",
    "ky niem" to "anniversary",
    "valentine" to "valentine",
    "tet" to "tet",
    "tan gia" to "housewarming",
    "mua dong" to "winter",
    "thang chuc" to "promotion"
)
private val StopRequirementTokens = setOf(
    "khong", "co", "yeu", "cau", "gi", "dac", "biet", "tang", "dip", "ngan", "sach", "bao", "nhieu", "cho", "va", "la", "de"
)
