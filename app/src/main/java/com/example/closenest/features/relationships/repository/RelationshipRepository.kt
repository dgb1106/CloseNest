package com.example.closenest.features.relationships.repository

import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipProfile
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    fun observeRelationships(): Flow<List<RelationshipProfile>>

    suspend fun addRelationship(request: NewRelationshipRequest)

    suspend fun deleteRelationship(relationshipId: String)

    suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest)
}
