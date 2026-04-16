package com.example.driver_management_system.data.service

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

class RoutingService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun getRoute(points: List<GeoPoint>): List<GeoPoint> = withContext(Dispatchers.IO) {
        try {
            if (points.size < 2) return@withContext points
            
            // Build coordinates string for OSRM API
            val coordinates = points.joinToString(";") { "${it.longitude},${it.latitude}" }
            
            // OSRM API endpoint (public demo server)
            val url = "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=polyline"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && body != null) {
                val osrmResponse = gson.fromJson(body, OSRMResponse::class.java)
                
                if (osrmResponse.code == "Ok" && osrmResponse.routes.isNotEmpty()) {
                    val geometry = osrmResponse.routes[0].geometry
                    return@withContext decodePolyline(geometry)
                }
            }
            
            // Fallback to straight lines if API fails
            points
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to straight lines
            points
        }
    }
    
    // Decode polyline string to list of GeoPoints
    private fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val point = GeoPoint(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(point)
        }

        return poly
    }
}

// OSRM API Response models
data class OSRMResponse(
    val code: String,
    val routes: List<Route>
)

data class Route(
    val geometry: String,
    val distance: Double,
    val duration: Double
)
