@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.0")

import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class OrderEntity(
    val id: String = "",
    val total: Double
)

try {
    val jsonString = """{"id": "123", "total": "100.00"}"""
    val order = Json.decodeFromString<OrderEntity>(jsonString)
    println("Success: $order")
} catch (e: Exception) {
    println("Error: ${e.message}")
}
