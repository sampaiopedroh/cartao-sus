package br.edu.cartaosus.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Distância em linha reta (fórmula de Haversine), em quilômetros.
object Distancia {
    const val CENTRO_SP_LAT = -23.5505
    const val CENTRO_SP_LON = -46.6333

    fun km(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val raio = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * raio * asin(sqrt(a))
    }
}
