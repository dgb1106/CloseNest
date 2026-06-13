package com.example.closenest.features.chatbot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.closenest.features.chatbot.repository.GiftCatalogService
import com.example.closenest.features.chatbot.repository.GiftReply
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GiftCatalogServiceTest {

    private lateinit var service: GiftCatalogService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        service = GiftCatalogService(context)
    }

    @Test
    fun `blank recipient query asks for recipient`() {
        val reply = service.buildGiftReply("", relationships())

        assertTrue(reply is GiftReply.AskForRecipient)
    }

    @Test
    fun `recipient lookup supports partial Vietnamese name`() {
        val relationship = relationships().first { it.name == "Minh Anh" }

        val reply = service.buildGiftReply("minh", relationships())

        assertTrue(reply is GiftReply.RecipientConfirmed)
        assertEquals(relationship.id, (reply as GiftReply.RecipientConfirmed).recipient.id)
    }

    @Test
    fun `unknown recipient returns not found without crashing`() {
        val reply = service.buildGiftReply("Nguoi la", relationships())

        assertTrue(reply is GiftReply.RecipientNotFound)
    }

    @Test
    fun `valid ai json creates recommendations and link messages from catalog only`() {
        val recipient = relationships().first { it.tag == RelationshipTag.Partner }
        val aiText = """
            {
              "intro": "Mình chọn theo sở thích của người nhận.",
              "items": [
                {"id": "gf_001", "reason": "món này hợp để trang trí góc học tập"},
                {"id": "gf_002", "reason": "món này hợp dịp kỷ niệm"}
              ]
            }
        """.trimIndent()

        val reply = service.buildRecommendationsReplyFromAi(recipient, "dịp kỷ niệm", aiText)

        assertTrue(reply is GiftReply.Recommendations)
        val recommendations = reply as GiftReply.Recommendations
        assertTrue(recommendations.summary.contains("Mình chọn theo sở thích của người nhận."))
        assertEquals(2, recommendations.linkMessages.size)
        assertTrue(recommendations.linkMessages.all { it.startsWith("Link mua ") })
        assertFalse(recommendations.summary.contains("shopee.vn"))
    }

    @Test
    fun `ai json with more than two valid items is capped at two recommendations`() {
        val recipient = relationships().first { it.tag == RelationshipTag.Partner }
        val aiText = """
            {
              "intro": "Mình chọn vài món phù hợp.",
              "items": [
                {"id": "gf_001", "reason": "phù hợp"},
                {"id": "gf_002", "reason": "dễ tặng"},
                {"id": "gf_003", "reason": "thiết thực"}
              ]
            }
        """.trimIndent()

        val reply = service.buildRecommendationsReplyFromAi(recipient, null, aiText)

        assertTrue(reply is GiftReply.Recommendations)
        assertEquals(2, (reply as GiftReply.Recommendations).linkMessages.size)
    }

    @Test
    fun `invalid ai json falls back to catalog recommendations`() {
        val recipient = relationships().first { it.tag == RelationshipTag.Partner }

        val reply = service.buildRecommendationsReplyFromAi(recipient, null, "not-json")

        assertTrue(reply is GiftReply.Recommendations)
        val recommendations = reply as GiftReply.Recommendations
        assertTrue(recommendations.summary.contains(recipient.name))
        assertTrue(recommendations.linkMessages.isNotEmpty())
    }

    @Test
    fun `ai json with unknown product ids falls back instead of exposing broken data`() {
        val recipient = relationships().first { it.tag == RelationshipTag.Partner }
        val aiText = """
            {
              "intro": "Mình chọn món này.",
              "items": [
                {"id": "unknown_id", "reason": "phù hợp"}
              ]
            }
        """.trimIndent()

        val reply = service.buildRecommendationsReplyFromAi(recipient, null, aiText)

        assertTrue(reply is GiftReply.Recommendations)
        val recommendations = reply as GiftReply.Recommendations
        assertFalse(recommendations.summary.contains("unknown_id"))
        assertTrue(recommendations.linkMessages.isNotEmpty())
    }

    private fun relationships(): List<RelationshipProfile> = listOf(
        relationship(id = "partner-1", name = "Minh Anh", tag = RelationshipTag.Partner),
        relationship(id = "friend-1", name = "Bao Nam", tag = RelationshipTag.Friend)
    )

    private fun relationship(
        id: String,
        name: String,
        tag: RelationshipTag
    ) = RelationshipProfile(
        id = id,
        userId = "user-1",
        name = name,
        tag = tag,
        birthdayIso = null,
        phoneNumber = null,
        email = null,
        interests = listOf("doc sach", "trang tri"),
        notes = "Thich qua thuc te",
        avatarUrl = null,
        priority = RelationshipPriority.High,
        createdAtMillis = 1_000L,
        updatedAtMillis = 2_000L
    )
}
