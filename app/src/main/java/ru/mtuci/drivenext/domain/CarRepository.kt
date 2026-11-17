package ru.mtuci.drivenext.domain

import java.math.BigDecimal

data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val pricePerDay: BigDecimal,
    val transmission: String,
    val fuelType: String,
    val imageUrl: String
)

interface CarRepository {
    suspend fun getCars(query: String = ""): List<Car>
}
