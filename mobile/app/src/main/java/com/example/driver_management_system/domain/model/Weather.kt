package com.example.driver_management_system.domain.model

data class Weather(
    val temperature: Double,
    val humidity: Int?,
    val windSpeed: Double,
    val windDirection: Int?,
    val rain: Double?,
    val weatherCode: Int,
    val description: String,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>
)

data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val rain: Double,
    val windSpeed: Double,
    val windDirection: Int?
)

data class DailyForecast(
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val weatherCode: Int,
    val description: String
)

// Weather code mapping based on WMO Weather interpretation codes
fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Cerah"
        1, 2, 3 -> "Cerah Berawan"
        45, 48 -> "Berkabut"
        51, 53, 55 -> "Gerimis"
        61, 63, 65 -> "Hujan"
        71, 73, 75 -> "Salju"
        77 -> "Hujan Es"
        80, 81, 82 -> "Hujan Lebat"
        85, 86 -> "Salju Lebat"
        95 -> "Badai Petir"
        96, 99 -> "Badai Petir dengan Hujan Es"
        else -> "Tidak Diketahui"
    }
}


// Wind direction mapping
fun getWindDirection(degrees: Int?): String {
    if (degrees == null) return "-"
    return when {
        degrees >= 337.5 || degrees < 22.5 -> "U" // Utara
        degrees >= 22.5 && degrees < 67.5 -> "TL" // Timur Laut
        degrees >= 67.5 && degrees < 112.5 -> "T" // Timur
        degrees >= 112.5 && degrees < 157.5 -> "TG" // Tenggara
        degrees >= 157.5 && degrees < 202.5 -> "S" // Selatan
        degrees >= 202.5 && degrees < 247.5 -> "BD" // Barat Daya
        degrees >= 247.5 && degrees < 292.5 -> "B" // Barat
        degrees >= 292.5 && degrees < 337.5 -> "BL" // Barat Laut
        else -> "-"
    }
}
