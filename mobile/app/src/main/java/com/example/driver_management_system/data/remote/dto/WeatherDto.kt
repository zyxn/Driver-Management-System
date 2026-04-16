package com.example.driver_management_system.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("current")
    val current: CurrentWeather,
    @SerializedName("hourly")
    val hourly: HourlyWeather,
    @SerializedName("daily")
    val daily: DailyWeather
)

data class CurrentWeather(
    @SerializedName("time")
    val time: String,
    @SerializedName("temperature_2m")
    val temperature: Double,
    @SerializedName("relative_humidity_2m")
    val humidity: Int? = null,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double,
    @SerializedName("wind_direction_10m")
    val windDirection: Int? = null,
    @SerializedName("rain")
    val rain: Double? = null
)

data class HourlyWeather(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature: List<Double>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>,
    @SerializedName("rain")
    val rain: List<Double>,
    @SerializedName("wind_speed_10m")
    val windSpeed: List<Double>,
    @SerializedName("wind_direction_10m")
    val windDirection: List<Int>? = null
)

data class DailyWeather(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("temperature_2m_max")
    val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min")
    val temperatureMin: List<Double>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>
)
