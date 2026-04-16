package com.example.driver_management_system.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver_management_system.domain.model.Weather
import com.example.driver_management_system.presentation.home.WeatherState
import com.example.driver_management_system.ui.theme.*

@Composable
fun WeatherCard(weatherState: WeatherState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        when (weatherState) {
            is WeatherState.Loading -> {
                WeatherSkeleton()
            }
            is WeatherState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Error",
                        tint = androidx.compose.ui.graphics.Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = weatherState.message,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
            is WeatherState.Success -> {
                WeatherContent(weather = weatherState.weather)
            }
        }
    }
}

@Composable
fun WeatherContent(weather: Weather) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cuaca Hari Ini",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Icon(
                imageVector = getWeatherIcon(weather.weatherCode),
                contentDescription = "Weather",
                tint = getWeatherIconColor(weather.weatherCode),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current Weather
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${weather.temperature.toInt()}°C",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = weather.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = "Wind",
                        tint = Blue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${weather.windSpeed.toInt()} km/h ${com.example.driver_management_system.domain.model.getWindDirection(weather.windDirection)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                if (weather.humidity != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = "Humidity",
                            tint = Blue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${weather.humidity}%",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                if (weather.rain != null && weather.rain > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Grain,
                            contentDescription = "Rain",
                            tint = Blue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${weather.rain} mm",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        HorizontalDivider(color = BorderBase)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Hourly Forecast
        Text(
            text = "Ramalan Per Jam",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            weather.hourlyForecast.take(12).forEach { hourly ->
                HourlyForecastItem(
                    time = hourly.time,
                    icon = getWeatherIcon(hourly.weatherCode),
                    temp = "${hourly.temperature.toInt()}°C",
                    rain = hourly.rain,
                    windSpeed = hourly.windSpeed.toInt(),
                    windDirection = com.example.driver_management_system.domain.model.getWindDirection(hourly.windDirection),
                    iconTint = getWeatherIconColor(hourly.weatherCode)
                )
            }
        }
    }
}

@Composable
fun HourlyForecastItem(
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    temp: String,
    rain: Double,
    windSpeed: Int,
    windDirection: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Text(
            text = time,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Icon(
            imageVector = icon,
            contentDescription = time,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = temp,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        // Wind info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Air,
                contentDescription = "Wind",
                tint = TextSecondary,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "$windSpeed $windDirection",
                fontSize = 9.sp,
                color = TextSecondary
            )
        }
        
        // Rain info (only show if > 0)
        if (rain > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Grain,
                    contentDescription = "Rain",
                    tint = Blue,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${rain}mm",
                    fontSize = 9.sp,
                    color = Blue
                )
            }
        }
    }
}

fun getWeatherIcon(code: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (code) {
        0 -> Icons.Outlined.WbSunny
        1, 2, 3 -> Icons.Outlined.Cloud
        45, 48 -> Icons.Outlined.Cloud
        51, 53, 55 -> Icons.Outlined.Grain
        61, 63, 65 -> Icons.Outlined.Grain
        71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
        80, 81, 82 -> Icons.Outlined.Grain
        95, 96, 99 -> Icons.Outlined.Thunderstorm
        else -> Icons.Outlined.Cloud
    }
}

fun getWeatherIconColor(code: Int): androidx.compose.ui.graphics.Color {
    return when (code) {
        0 -> Orange
        1, 2, 3 -> TextSecondary
        45, 48 -> TextSecondary
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Blue
        71, 73, 75, 77, 85, 86 -> androidx.compose.ui.graphics.Color(0xFF87CEEB)
        95, 96, 99 -> androidx.compose.ui.graphics.Color(0xFF4A5568)
        else -> TextSecondary
    }
}


@Composable
fun WeatherSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Header Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                width = 100.dp,
                height = 16.dp
            )
            SkeletonBox(
                width = 20.dp,
                height = 20.dp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current Weather Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    width = 80.dp,
                    height = 36.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(
                    width = 120.dp,
                    height = 14.dp
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                SkeletonBox(
                    width = 70.dp,
                    height = 16.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(
                    width = 50.dp,
                    height = 16.dp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        HorizontalDivider(color = BorderBase)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Hourly Forecast Skeleton
        SkeletonBox(
            width = 120.dp,
            height = 14.dp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    SkeletonBox(width = 40.dp, height = 12.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(width = 18.dp, height = 18.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(width = 35.dp, height = 12.dp)
                }
            }
        }
    }
}

@Composable
fun SkeletonBox(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shimmerEffect()
    )
}

fun Modifier.shimmerEffect(): Modifier = this.then(
    background(
        androidx.compose.ui.graphics.Color(0xFFE0E0E0),
        shape = RoundedCornerShape(4.dp)
    )
)
