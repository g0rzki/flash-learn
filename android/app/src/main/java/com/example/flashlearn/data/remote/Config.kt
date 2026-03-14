package com.example.flashlearn.data.remote

import android.content.Context
import java.util.Properties

object Config {
    private var baseUrl: String? = null

    fun init(context: Context) {
        if (baseUrl == null) {
            val properties = Properties()
            try {
                context.assets.open("config.properties").use { inputStream ->
                    properties.load(inputStream)
                }
                baseUrl = properties.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")
            } catch (e: Exception) {
                baseUrl = "http://10.0.2.2:8080/"
            }
        }
    }

    fun getBaseUrl(): String {
        return baseUrl ?: "http://10.0.2.2:8080/"
    }
}
