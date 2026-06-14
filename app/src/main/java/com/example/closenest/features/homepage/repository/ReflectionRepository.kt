package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.MoodDayEntry
import com.example.closenest.features.homepage.model.NewReflectionRequest

interface ReflectionRepository {
    suspend fun addReflection(request: NewReflectionRequest)
    suspend fun getRecentMoods(days: Int): Result<List<MoodDayEntry>>
}
