package br.edu.cartaosus.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpGeneratorTest {
    // Vetor oficial da RFC 6238: segredo ASCII "12345678901234567890".
    private val segredoRfc = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun vetorDaRfcNoInstante59SegundosDaSeis287082() =
        assertEquals("287082", TotpGenerator.codigo(segredoRfc, 59_000L))

    @Test
    fun codigoTemSeisDigitos() {
        val codigo = TotpGenerator.codigo(segredoRfc, 1_700_000_000_000L)
        assertEquals(6, codigo.length)
        assertTrue(codigo.all { it.isDigit() })
    }

    @Test
    fun codigoMudaACadaPasso() {
        val a = TotpGenerator.codigo(segredoRfc, 1_700_000_000_000L)
        val b = TotpGenerator.codigo(segredoRfc, 1_700_000_030_000L)
        assertFalse(a == b)
    }

    @Test
    fun mesmoCodigoDentroDoMesmoPasso() {
        // 1_700_000_010 é início de passo (múltiplo de 30 s): +29 s continua no mesmo passo.
        val a = TotpGenerator.codigo(segredoRfc, 1_700_000_010_000L)
        val b = TotpGenerator.codigo(segredoRfc, 1_700_000_039_000L)
        assertEquals(a, b)
    }

    @Test
    fun restanteEhDeUmAteTrinta() {
        assertEquals(30, TotpGenerator.restanteSegundos(0L))
        assertEquals(29, TotpGenerator.restanteSegundos(1_000L))
        assertEquals(1, TotpGenerator.restanteSegundos(29_000L))
    }

    @Test
    fun segredoComFormatoValidoEhAceito() = assertTrue(TotpGenerator.segredoValido("gezd gnbv gy3t qojq"))

    @Test
    fun segredoComCaracteresInvalidosEhRecusado() = assertFalse(TotpGenerator.segredoValido("GEZD1NBV"))
}
