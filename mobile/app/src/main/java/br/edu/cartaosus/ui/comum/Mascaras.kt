package br.edu.cartaosus.ui.comum

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Máscara visual: o valor guardado são só dígitos; o campo mostra "###.###.###-##" e similares.
class MascaraVisual(private val padrao: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        val saida = aplicar(padrao, digitos)
        val mapa = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var contados = 0
                var posicao = 0
                while (posicao < padrao.length && contados < offset) {
                    if (padrao[posicao] == '#') contados++
                    posicao++
                }
                return minOf(posicao, saida.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var contados = 0
                for (p in 0 until minOf(offset, padrao.length)) if (padrao[p] == '#') contados++
                return minOf(contados, digitos.length)
            }
        }
        return TransformedText(AnnotatedString(saida), mapa)
    }

    companion object {
        const val CPF = "###.###.###-##"
        const val CNS = "### #### #### ####"
        const val DATA = "##/##/####"

        fun aplicar(padrao: String, digitos: String): String {
            val sb = StringBuilder()
            var i = 0
            for (c in padrao) {
                if (i >= digitos.length) break
                if (c == '#') {
                    sb.append(digitos[i]); i++
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
