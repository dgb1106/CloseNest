package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.NewReflectionRequest

interface ReflectionRepository {
    suspend fun addReflection(request: NewReflectionRequest)
}
