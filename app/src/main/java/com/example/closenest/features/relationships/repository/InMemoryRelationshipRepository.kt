package com.example.closenest.features.relationships.repository

import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryRelationshipRepository : RelationshipRepository {
    private val demoUserId = "demo-user"
    private val relationships = MutableStateFlow(seedRelationships())

    override fun observeRelationships(): Flow<List<RelationshipProfile>> = relationships.asStateFlow()

    override suspend fun addRelationship(request: NewRelationshipRequest) {
        val now = System.currentTimeMillis()
        val newRelationship = RelationshipProfile(
            id = UUID.randomUUID().toString(),
            userId = demoUserId,
            name = request.name,
            tag = request.tag,
            birthdayIso = request.birthdayIso,
            phoneNumber = request.phoneNumber,
            email = request.email,
            interests = request.interests,
            notes = request.notes,
            avatarUrl = null,
            priority = request.priority,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        relationships.update { current ->
            (current + newRelationship).sortedBy { it.name.lowercase() }
        }
    }

    private fun seedRelationships(): List<RelationshipProfile> {
        val now = System.currentTimeMillis()
        return listOf(
            RelationshipProfile(
                id = UUID.randomUUID().toString(),
                userId = demoUserId,
                name = "Minh Anh",
                tag = RelationshipTag.CloseFriend,
                birthdayIso = "2003-11-12",
                phoneNumber = "0901234567",
                email = "minhanh@example.com",
                interests = listOf("Cà phê", "Ảnh film"),
                notes = "Hay đi bộ buổi tối và thích những lời nhắn ngắn gọn.",
                avatarUrl = null,
                priority = RelationshipPriority.High,
                createdAtMillis = now - 120.daysInMillis,
                updatedAtMillis = now - 5.daysInMillis
            ),
            RelationshipProfile(
                id = UUID.randomUUID().toString(),
                userId = demoUserId,
                name = "Gia Hân",
                tag = RelationshipTag.Family,
                birthdayIso = "2000-06-21",
                phoneNumber = "0988123456",
                email = null,
                interests = listOf("Bếp núc", "Du lịch"),
                notes = "Thường rảnh sau 20h, hợp để gọi điện cuối tuần.",
                avatarUrl = null,
                priority = RelationshipPriority.High,
                createdAtMillis = now - 260.daysInMillis,
                updatedAtMillis = now - 2.daysInMillis
            ),
            RelationshipProfile(
                id = UUID.randomUUID().toString(),
                userId = demoUserId,
                name = "Bảo Nam",
                tag = RelationshipTag.Classmate,
                birthdayIso = null,
                phoneNumber = null,
                email = "baonam@example.com",
                interests = listOf("Android", "Board game"),
                notes = null,
                avatarUrl = null,
                priority = RelationshipPriority.Medium,
                createdAtMillis = now - 80.daysInMillis,
                updatedAtMillis = now - 18.daysInMillis
            )
        ).sortedBy { it.name.lowercase() }
    }

    private val Int.daysInMillis: Long
        get() = this * 24L * 60L * 60L * 1_000L
}
