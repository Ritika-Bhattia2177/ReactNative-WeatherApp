package com.example.weathernow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherNowApp()
                }
            }
        }
    }
}

data class WeatherResult(
    val city: String,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val condition: String
)

@Composable
fun WeatherNowApp() {

    var city by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf<WeatherResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "WeatherNow",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check weather anywhere",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = city,
            onValueChange = {
                city = it
                errorMessage = ""
            },
            label = {
                Text("Enter city")
            },
            placeholder = {
                Text("Example: Delhi")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {

                if (city.isBlank()) {
                    errorMessage = "Please enter a city name"
                    return@Button
                }

                scope.launch {

                    isLoading = true
                    errorMessage = ""
                    weather = null

                    try {
                        weather = getWeather(city.trim())
                    } catch (e: Exception) {
                        errorMessage =
                            "Unable to get weather. Please check the city name or internet connection."
                    }

                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Search Weather")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        weather?.let { result ->

            Spacer(modifier = Modifier.height(16.dp))

            WeatherCard(result)
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherResult) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(24.dp)
        ) {

            Text(
                text = weather.city,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${weather.temperature} °C",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = weather.condition,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text("Humidity")
                    Text(
                        "${weather.humidity}%",
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("Wind")
                    Text(
                        "${weather.windSpeed} km/h",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

suspend fun getWeather(city: String): WeatherResult {

    return withContext(Dispatchers.IO) {

        val encodedCity = URLEncoder.encode(city, "UTF-8")

        val geocodingUrl =
            "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encodedCity&count=1&language=en&format=json"

        val geoConnection =
            URL(geocodingUrl).openConnection() as HttpURLConnection

        geoConnection.requestMethod = "GET"
        geoConnection.connectTimeout = 10000
        geoConnection.readTimeout = 10000

        val geoResponse =
            geoConnection.inputStream.bufferedReader().use {
                it.readText()
            }

        geoConnection.disconnect()

        val geoJson = JSONObject(geoResponse)

        if (!geoJson.has("results")) {
            throw Exception("City not found")
        }

        val results = geoJson.getJSONArray("results")

        if (results.length() == 0) {
            throw Exception("City not found")
        }

        val location = results.getJSONObject(0)

        val latitude = location.getDouble("latitude")
        val longitude = location.getDouble("longitude")
        val cityName = location.getString("name")

        val weatherUrl =
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                    "&timezone=auto"

        val weatherConnection =
            URL(weatherUrl).openConnection() as HttpURLConnection

        weatherConnection.requestMethod = "GET"
        weatherConnection.connectTimeout = 10000
        weatherConnection.readTimeout = 10000

        val weatherResponse =
            weatherConnection.inputStream.bufferedReader().use {
                it.readText()
            }

        weatherConnection.disconnect()

        val weatherJson = JSONObject(weatherResponse)
        val current = weatherJson.getJSONObject("current")

        val temperature =
            current.getDouble("temperature_2m")

        val humidity =
            current.getInt("relative_humidity_2m")

        val windSpeed =
            current.getDouble("wind_speed_10m")

        val weatherCode =
            current.getInt("weather_code")

        WeatherResult(
            city = cityName,
            temperature = temperature,
            humidity = humidity,
            windSpeed = windSpeed,
            condition = getWeatherCondition(weatherCode)
        )
    }
}

fun getWeatherCondition(code: Int): String {

    return when (code) {

        0 -> "Clear sky"

        1, 2, 3 -> "Partly cloudy"

        45, 48 -> "Foggy"

        51, 53, 55 -> "Drizzle"

        61, 63, 65 -> "Rain"

        71, 73, 75 -> "Snow"

        80, 81, 82 -> "Rain showers"

        95, 96, 99 -> "Thunderstorm"

        else -> "Unknown"
    }
}