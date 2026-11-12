package br.edu.cartaosus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidacaoTest {

    @Test
    fun emailValidoEhAceito() = assertNull(Validacao.email("maria@cartaosus.demo"))

    @Test
    fun emailSemArrobaEhRecusado() = assertEquals("E-mail inválido.", Validacao.email("maria.sem-arroba"))

    @Test
    fun senhaComOitoCaracteresEhAceita() = assertNull(Validacao.senha("12345678"))

    @Test
    fun senhaCurtaEhRecusada() = assertEquals("A senha deve ter pelo menos 8 caracteres.", Validacao.senha("1234567"))

    @Test
    fun cpfComOnzeDigitosEhAceito() = assertNull(Validacao.cpf("52998224725"))

    @Test
    fun cpfComLetrasEhRecusado() = assertEquals("CPF deve ter 11 dígitos.", Validacao.cpf("5299822472A"))

    @Test
    fun cnsComQuinzeDigitosEhAceito() = assertNull(Validacao.cns("898000000000000"))

    @Test
    fun cnsCurtoEhRecusado() = assertEquals("CNS deve ter 15 dígitos.", Validacao.cns("8980000"))

    @Test
    fun nascimentoNoPassadoEhAceito() =
        assertNull(Validacao.nascimento("1990-05-20", hoje = LocalDate.of(2026, 11, 11)))

    @Test
    fun nascimentoNoFuturoEhRecusado() =
        assertEquals("A data deve estar no passado.", Validacao.nascimento("2030-01-01", hoje = LocalDate.of(2026, 11, 11)))

    @Test
    fun nascimentoEmFormatoErradoEhRecusado() =
        assertEquals("Use o formato AAAA-MM-DD.", Validacao.nascimento("11/11/1990", hoje = LocalDate.of(2026, 11, 11)))
}

class ValidacaoDataBrTest {
    private val hoje = LocalDate.of(2026, 11, 11)

    @Test
    fun dataComOitoDigitosValidaEhAceita() = assertNull(Validacao.nascimentoDigitos("15031950", hoje))

    @Test
    fun dataComMenosDeOitoDigitosPedeOsOitoNumeros() =
        assertEquals("Digite a data com 8 números: dia, mês e ano.", Validacao.nascimentoDigitos("1503195", hoje))

    @Test
    fun dataInexistenteEhRecusada() =
        assertEquals("Use o formato AAAA-MM-DD.", Validacao.nascimentoDigitos("31022000", hoje))

    @Test
    fun dataFuturaEhRecusada() =
        assertEquals("A data deve estar no passado.", Validacao.nascimentoDigitos("01012030", hoje))

    @Test
    fun conversaoParaIsoUsaOFormatoDaApi() = assertEquals("1950-03-15", Validacao.paraIso("15031950"))
}
