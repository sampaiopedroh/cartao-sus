package br.edu.cartaosus.ui.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

// Contraste entre texto e fundo (WCAG 2.1): 4,5:1 para texto comum (nível AA).
// Conferido nos temas claro e escuro, para todos os pares de cor usados no app.
class ContrasteTest {
    private fun contraste(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun pares(c: ColorScheme) = mapOf(
        "texto no fundo" to (c.onBackground to c.background),
        "texto no cartão" to (c.onSurface to c.surface),
        "texto secundário no cartão" to (c.onSurfaceVariant to c.surface),
        "texto secundário no fundo" to (c.onSurfaceVariant to c.background),
        "link/botão no fundo" to (c.primary to c.background),
        "link/botão no cartão" to (c.primary to c.surface),
        "texto do botão" to (c.onPrimary to c.primary),
        "aviso informativo" to (c.onPrimaryContainer to c.primaryContainer),
        "aviso de sucesso" to (c.onSecondaryContainer to c.secondaryContainer),
        "aviso de atenção" to (c.onTertiaryContainer to c.tertiaryContainer),
        "aviso de erro" to (c.onErrorContainer to c.errorContainer),
        "texto de erro no cartão" to (c.error to c.surface),
        "texto do botão de erro" to (c.onError to c.error),
    )

    private fun verificar(nome: String, c: ColorScheme) {
        val falhas = pares(c).mapNotNull { (par, cores) ->
            val r = contraste(cores.first, cores.second)
            if (r < 4.5) "$par: %.2f".format(r) else null
        }
        assertTrue("Contraste abaixo de 4,5:1 no tema $nome: $falhas", falhas.isEmpty())
    }

    @Test fun `tema claro atende WCAG AA`() = verificar("claro", Claro)

    @Test fun `tema escuro atende WCAG AA`() = verificar("escuro", Escuro)
}
