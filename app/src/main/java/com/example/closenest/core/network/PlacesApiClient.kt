package com.example.closenest.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PlacesApiClient {
    private const val TAG = "PlacesApiClient"
    private const val BASE_URL = "https://api.mapbox.com/search/searchbox/v1"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000

    data class PlaceSuggestion(
        val mapboxId: String,
        val name: String,
        val fullAddress: String,
        val placeFormatted: String
    )

    data class PlaceResult(
        val fullAddress: String,
        val latitude: Double,
        val longitude: Double
    )

    suspend fun suggest(
        query: String,
        accessToken: String,
        sessionToken: String,
        country: String = "VN",
        language: String = "vi",
        limit: Int = 5
    ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "$BASE_URL/suggest?q=$encodedQuery" +
                "&access_token=$accessToken" +
                "&session_token=$sessionToken" +
                "&country=$country" +
                "&language=$language" +
                "&limit=$limit"
        )
        Log.d(TAG, "suggest query='$query' url='${url}'")

        try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "suggest failed code=$responseCode body='$errorBody'")
                connection.disconnect()
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val parsed = parseSuggestions(JSONObject(response))
            Log.d(TAG, "suggest success count=${parsed.size}")
            parsed
        } catch (e: IOException) {
            Log.e(TAG, "suggest IOException", e)
            emptyList()
        }
    }

    suspend fun retrieve(
        mapboxId: String,
        accessToken: String,
        sessionToken: String
    ): PlaceResult? = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/retrieve/$mapboxId?access_token=$accessToken&session_token=$sessionToken")
        Log.d(TAG, "retrieve mapboxId='$mapboxId' url='${url}'")

        try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "retrieve failed code=$responseCode body='$errorBody'")
                connection.disconnect()
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val parsed = parseRetrieve(JSONObject(response))
            Log.d(TAG, "retrieve success found=${parsed != null}")
            parsed
        } catch (e: IOException) {
            Log.e(TAG, "retrieve IOException", e)
            null
        }
    }

    private fun parseSuggestions(json: JSONObject): List<PlaceSuggestion> {
        val suggestionsArray = json.optJSONArray("suggestions") ?: return emptyList()
        val results = mutableListOf<PlaceSuggestion>()
        for (i in 0 until suggestionsArray.length()) {
            val item = suggestionsArray.getJSONObject(i)
            val name = item.optString("name", "")
            val fullAddress = item.optString("full_address", "")
            val placeFormatted = item.optString("place_formatted", "")
            val mapboxId = item.optString("mapbox_id", "")
            if (mapboxId.isNotBlank() && name.isNotBlank()) {
                results.add(
                    PlaceSuggestion(
                        mapboxId = mapboxId,
                        name = name,
                        fullAddress = fullAddress.ifBlank { placeFormatted },
                        placeFormatted = placeFormatted.ifBlank { name }
                    )
                )
            }
        }
        return results
    }

    private fun parseRetrieve(json: JSONObject): PlaceResult? {
        val features = json.optJSONArray("features") ?: return null
        if (features.length() == 0) return null

        val feature = features.getJSONObject(0)
        val properties = feature.optJSONObject("properties") ?: return null
        val geometry = feature.optJSONObject("geometry") ?: return null
        val coordinates = geometry.optJSONArray("coordinates") ?: return null

        if (coordinates.length() < 2) return null

        val fullAddress = properties.optString("full_address",
            properties.optString("place_formatted", ""))

        return PlaceResult(
            fullAddress = fullAddress,
            latitude = coordinates.getDouble(1),
            longitude = coordinates.getDouble(0)
        )
    }
}
