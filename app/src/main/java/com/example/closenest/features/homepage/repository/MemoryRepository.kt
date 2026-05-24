package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.NewMemoryRequest

interface MemoryRepository {
    suspend fun addMemory(request: NewMemoryRequest)
}
