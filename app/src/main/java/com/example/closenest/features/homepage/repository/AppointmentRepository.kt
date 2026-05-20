package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.NewAppointmentRequest

interface AppointmentRepository {
    suspend fun addAppointment(request: NewAppointmentRequest)
}
