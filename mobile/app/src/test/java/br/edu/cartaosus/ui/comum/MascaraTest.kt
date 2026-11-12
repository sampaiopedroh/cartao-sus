package br.edu.cartaosus.ui.comum

import org.junit.Assert.assertEquals
import org.junit.Test

class MascaraTest {
    @Test
    fun cpfCompletoGanhaPontosETraco() = assertEquals("529.982.247-25", MascaraVisual.aplicar(MascaraVisual.CPF, "52998224725"))

    @Test
    fun cpfParcialNaoMostraSeparadorSobrando() = assertEquals("529.98", MascaraVisual.aplicar(MascaraVisual.CPF, "52998"))

    @Test
    fun cnsSeparaEmGrupos() = assertEquals("898 0000 0000 0000", MascaraVisual.aplicar(MascaraVisual.CNS, "898000000000000"))

    @Test
    fun dataVira15Barra03Barra1950() = assertEquals("15/03/1950", MascaraVisual.aplicar(MascaraVisual.DATA, "15031950"))
}
