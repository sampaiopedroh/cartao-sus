package br.edu.cartaosus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassificacaoTest {
    @Test
    fun pressaoNormalEh120Por80() = assertEquals(Nivel.NORMAL, Classificacao.pressao(120.0, 80.0).nivel)

    @Test
    fun pressaoElevadaEhDeAtencao() = assertEquals(Nivel.ATENCAO, Classificacao.pressao(150.0, 95.0).nivel)

    @Test
    fun sistolicaAcimaDe180EhCritica() = assertEquals(Nivel.CRITICO, Classificacao.pressao(185.0, 100.0).nivel)

    @Test
    fun diastolicaAcimaDe110EhCritica() = assertEquals(Nivel.CRITICO, Classificacao.pressao(130.0, 115.0).nivel)

    @Test
    fun limiteExatoDe180NaoEhCritico() = assertEquals(Nivel.ATENCAO, Classificacao.pressao(180.0, 100.0).nivel)

    @Test
    fun glicemiaDe300OuMaisEhCritica() = assertEquals(Nivel.CRITICO, Classificacao.glicemia(300.0).nivel)

    @Test
    fun glicemiaNormalNaoAlerta() = assertEquals(Nivel.NORMAL, Classificacao.glicemia(110.0).nivel)
}
