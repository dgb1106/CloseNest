package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.AppointmentItem
import com.example.closenest.features.homepage.model.NewAppointmentRequest

interface AppointmentRepository {
    suspend fun addAppointment(request: NewAppointmentRequest)
    suspend fun countUpcomingAppointments(todayMillis: Long = System.currentTimeMillis()): Int
    suspend fun getUpcomingAppointments(todayMillis: Long = System.currentTimeMillis()): List<AppointmentItem>
    suspend fun deleteAppointment(appointmentId: String)
}
