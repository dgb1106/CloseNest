package com.example.closenest.features.relationships.model

import androidx.annotation.StringRes
import com.example.closenest.R

data class RelationshipProfile(
    val id: String,
    val userId: String,
    val name: String,
    val tag: RelationshipTag,
    val birthdayIso: String?,
    val phoneNumber: String?,
    val email: String?,
    val interests: List<String>,
    val notes: String?,
    val avatarUrl: String?,
    val priority: RelationshipPriority,
    val lastInteractionType: RecentInteractionType?,
    val lastInteractionAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class NewRelationshipRequest(
    val name: String,
    val tag: RelationshipTag,
    val birthdayIso: String?,
    val phoneNumber: String?,
    val email: String?,
    val interests: List<String>,
    val notes: String?,
    val priority: RelationshipPriority
)

enum class RelationshipTag(
    @param:StringRes val labelRes: Int,
    val followUpThresholdDays: Long
) {
    Family(R.string.relationship_tag_family, 10),
    Friend(R.string.relationship_tag_friend, 14),
    CloseFriend(R.string.relationship_tag_close_friend, 7),
    Classmate(R.string.relationship_tag_classmate, 14),
    Coworker(R.string.relationship_tag_coworker, 21),
    Mentor(R.string.relationship_tag_mentor, 18),
    Partner(R.string.relationship_tag_partner, 3),
    Other(R.string.relationship_tag_other, 30)
}

enum class RelationshipPriority(
    @param:StringRes val labelRes: Int,
    val value: Int
) {
    Low(R.string.relationship_priority_low, 1),
    Medium(R.string.relationship_priority_medium, 2),
    High(R.string.relationship_priority_high, 3)
}

enum class RecentInteractionType(
    @param:StringRes val labelRes: Int
) {
    Meet(R.string.interaction_type_meet),
    Chat(R.string.interaction_type_chat),
    Call(R.string.interaction_type_call),
    Message(R.string.interaction_type_message),
    Gift(R.string.interaction_type_gift),
    Date(R.string.interaction_type_date),
    Other(R.string.interaction_type_other)
}

enum class AttentionStatus(
    @param:StringRes val labelRes: Int
) {
    NeedsAttention(R.string.attention_needs_attention),
    Warm(R.string.attention_warm),
    RecentlyConnected(R.string.attention_recently_connected)
}
