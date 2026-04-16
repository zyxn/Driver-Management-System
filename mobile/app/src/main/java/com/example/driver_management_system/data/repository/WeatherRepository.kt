package com.example.driver_management_system.data.repository

import android.util.Log
import com.example.driver_management_system.data.remote.WeatherApiService
import com.example.driver_management_system.data.remote.dto.WeatherResponse
import com.example.driver_management_system.domain.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository {
    private val weatherApi: WeatherApiService
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        weatherApi = retrofit.create(WeatherApiService::class.java)
    }
    
    suspend fun getWeather(latitude: Double, longitude: Double): Result<Weather> {
        return try {
            Log.d("WeatherRepository", "Calling API with lat=$latitude, lon=$longitude")
            val response = weatherApi.getWeatherForecast(latitude, longitude)
            Log.d("WeatherRepository", "API response received")
            val weather = mapToWeather(response)
            Log.d("WeatherRepository", "Weather mapped successfully")
            Result.success(weather)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "API call failed", e)
            Result.failure(e)
        }
    }
    
    private fun mapToWeather(response: WeatherResponse): Weather {
        // Get current time
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        
        // Filter hourly forecast to only show current hour and future hours
        val hourlyForecast = response.hourly.time.mapIndexed { index, time ->
            val hourTime = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val parsedDate = inputFormat.parse(time)
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate ?: Date()
                calendar.get(Calendar.HOUR_OF_DAY)
            } catch (e: Exception) {
                0
            }
            
            Pair(hourTime, HourlyForecast(
                time = formatHourlyTime(time),
                temperature = response.hourly.temperature[index],
                weatherCode = response.hourly.weatherCode[index],
                rain = response.hourly.rain[index],
                windSpeed = response.hourly.windSpeed[index],
                windDirection = response.hourly.windDirection?.getOrNull(index)
            ))
        }.filter { (hour, _) ->
            // Only include current hour and future hours
            hour >= currentHour
        }.map { (_, forecast) ->
            forecast
        }.take(12) // Take only 12 hours
        
        val dailyForecast = response.daily.time.take(7).mapIndexed { index, date ->
            DailyForecast(
                date = formatDailyDate(date),
                temperatureMax = response.daily.temperatureMax[index],
                temperatureMin = response.daily.temperatureMin[index],
                weatherCode = response.daily.weatherCode[index],
                description = getWeatherDescription(response.daily.weatherCode[index])
            )
        }
        
        return Weather(
            temperature = response.current.temperature,
            humidity = response.current.humidity,
            windSpeed = response.current.windSpeed,
            windDirection = response.current.windDirection,
            rain = response.current.rain,
            weatherCode = response.current.weatherCode,
            description = getWeatherDescription(response.current.weatherCode),
            hourlyForecast = hourlyForecast,
            dailyForecast = dailyForecast
        )
    }
    
    private fun formatHourlyTime(time: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(time)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            time
        }
    }
    
    private fun formatDailyDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate ?: Date())
        } catch (e: Exception) {
            date
        }
    }
}
