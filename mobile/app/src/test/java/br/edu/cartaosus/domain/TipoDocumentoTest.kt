package br.edu.cartaosus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TipoDocumentoTest {
    @Test fun `tipo desconhecido vira OUTRO`() = assertEquals(TipoDocumento.OUTRO, TipoDocumento.de("XYZ"))

    @Test fun `tipo conhecido e reconhecido`() = assertEquals(TipoDocumento.RG, TipoDocumento.de("RG"))

    @Test fun `titulo sugerido usa o nome do arquivo sem extensao`() =
        assertEquals("laudo_cirurgia", TipoDocumento.tituloSugerido("laudo_cirurgia.pdf", TipoDocumento.LAUDO))

    @Test fun `titulo sugerido cai no rotulo do tipo quando o nome e curto ou ausente`() {
        assertEquals("Receita de remédio", TipoDocumento.tituloSugerido(null, TipoDocumento.RECEITA))
        assertEquals("RG", TipoDocumento.tituloSugerido("a.p", TipoDocumento.RG))
    }
}
