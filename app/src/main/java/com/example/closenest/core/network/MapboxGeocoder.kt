package com.example.closenest.core.network

import com.mapbox.geojson.Point
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class MapboxGeocodingResult(
    val name: String,
    val fullAddress: String,
    val point: Point
)

suspend fun searchMapboxLocations(
    query: String,
    accessToken: String,
    limit: Int = 5,
    language: String = "vi"
): List<MapboxGeocodingResult> = withContext(Dispatchers.IO) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = URL(
        "https://api.mapbox.com/geocoding/v5/mapbox.places/" +
            "$encodedQuery.json?access_token=$accessToken&limit=$limit&language=$language"
    )
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000

        val response = connection.inputStream.bufferedReader().readText()
        val features = JSONObject(response).getJSONArray("features")

        (0 until features.length()).map { index ->
            val feature = features.getJSONObject(index)
            val center = feature.getJSONArray("center")

            MapboxGeocodingResult(
                name = feature.getString("text"),
                fullAddress = feature.getString("place_name"),
                point = Point.fromLngLat(center.getDouble(0), center.getDouble(1))
            )
        }
    } finally {
        connection.disconnect()
    }
}
