package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.NewInteractionLogRequest

interface InteractionLogRepository {
    suspend fun addInteractionLog(request: NewInteractionLogRequest)
}
