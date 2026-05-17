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
    @param:StringRes val labelRes: Int
) {
    Family(R.string.relationship_tag_family),
    Friend(R.string.relationship_tag_friend),
    CloseFriend(R.string.relationship_tag_close_friend),
    Classmate(R.string.relationship_tag_classmate),
    Coworker(R.string.relationship_tag_coworker),
    Mentor(R.string.relationship_tag_mentor),
    Partner(R.string.relationship_tag_partner),
    Other(R.string.relationship_tag_other)
}

val SelectableRelationshipTags = listOf(
    RelationshipTag.Family,
    RelationshipTag.Friend,
    RelationshipTag.CloseFriend,
    RelationshipTag.Classmate
)

enum class RelationshipPriority(
    @param:StringRes val labelRes: Int,
    val value: Int
) {
    Low(R.string.relationship_priority_low, 1),
    Medium(R.string.relationship_priority_medium, 2),
    High(R.string.relationship_priority_high, 3)
}
