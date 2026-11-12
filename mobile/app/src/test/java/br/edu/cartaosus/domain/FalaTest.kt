package br.edu.cartaosus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FalaTest {
    @Test fun `codigo e soletrado digito por digito`() = assertEquals("1 2 3 4 5 6", Fala.soletrar("123 456"))

    @Test fun `cns e falado em grupos de 3 e 4 digitos`() =
        assertEquals("8 9 8, 0 0 0 0, 1 2 3 4, 5 6 7 8", Fala.cns("898000012345678"))

    @Test fun `segredo e falado em blocos de quatro`() = assertEquals("A B C D, E F G H", Fala.segredo("ABCDEFGH"))

    @Test fun `medida de pressao vira frase`() =
        assertEquals("124 por 80 milímetros de mercúrio, registrado por você", Fala.medida("124/80 mmHg · você"))

    @Test fun `glicemia e peso por extenso`() {
        assertEquals("112 miligramas por decilitro", Fala.medida("112 mg/dL"))
        assertEquals("70,5 quilos", Fala.medida("70,5 kg"))
    }

    @Test fun `resumo do grafico traz recente, menor, maior e tendencia`() {
        val r = Fala.resumoSerie("glicemia", "miligramas por decilitro", listOf(100.0, 140.0, 120.0))
        assertTrue(r.contains("3 medições"))
        assertTrue(r.contains("Mais recente 120"))
        assertTrue(r.contains("Menor 100, maior 140"))
        assertTrue(r.contains("subindo"))
    }

    @Test fun `resumo sem medicoes`() = assertEquals("Gráfico de peso sem medições.", Fala.resumoSerie("peso", "quilos", emptyList()))
}
