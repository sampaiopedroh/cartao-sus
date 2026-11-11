package br.edu.cartaosus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanciaTest {
    @Test
    fun mesmoPontoDaZeroKm() = assertEquals(0.0, Distancia.km(-23.55, -46.63, -23.55, -46.63), 1e-9)

    @Test
    fun umGrauDeLatitudeEquivaleACercaDe111Km() {
        val km = Distancia.km(0.0, 0.0, 1.0, 0.0)
        assertTrue("obtido $km", km in 110.0..112.0)
    }
}

class GraficoTest {
    @Test
    fun normalizaEntreZeroEUm() = assertEquals(listOf(0f, 0.5f, 1f), Grafico.normalizar(listOf(1.0, 2.0, 3.0)))

    @Test
    fun valoresIguaisFicamNoMeio() = assertEquals(listOf(0.5f, 0.5f), Grafico.normalizar(listOf(4.0, 4.0)))

    @Test
    fun listaVaziaNaoTemPontos() = assertTrue(Grafico.normalizar(emptyList()).isEmpty())

    @Test
    fun menosDeDoisPontosSaoPoucosDados() {
        assertTrue(Grafico.poucosDados(1))
        assertEquals(false, Grafico.poucosDados(2))
    }
}
