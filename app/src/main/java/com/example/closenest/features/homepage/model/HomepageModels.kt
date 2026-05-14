package com.example.closenest.features.homepage.model

data class MapMarker(
    val label: String,
    val x: Float,
    val y: Float,
    val highlighted: Boolean
)

val DemoMarkers = listOf(
    MapMarker("A", 0.16f, 0.24f, false),
    MapMarker("B", 0.48f, 0.14f, false),
    MapMarker("C", 0.85f, 0.24f, false),
    MapMarker("D", 0.12f, 0.54f, false),
    MapMarker("E", 0.32f, 0.68f, false),
    MapMarker("F", 0.73f, 0.57f, false),
    MapMarker("JT", 0.51f, 0.43f, true)
)
