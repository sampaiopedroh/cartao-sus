package br.edu.cartaosus.domain

// Textos pensados para leitores de tela (TalkBack): números e códigos lidos dígito por dígito,
// e resumos falados de informações que na tela são visuais (gráfico, cartão).
object Fala {
    // "123456" -> "1 2 3 4 5 6": o leitor fala cada dígito, e não "cento e vinte e três mil...".
    fun soletrar(texto: String): String = texto.filter { !it.isWhitespace() }.toList().joinToString(" ")

    // CNS em grupos (3 + 4 + 4 + 4), cada grupo soletrado e separado por vírgula para dar uma pausa.
    fun cns(cns: String): String {
        val d = cns.filter { it.isDigit() }
        if (d.length != 15) return soletrar(d)
        return listOf(d.take(3), d.substring(3, 7), d.substring(7, 11), d.substring(11)).joinToString(", ") { soletrar(it) }
    }

    // Código secreto (Base32) em blocos de 4, para a pessoa conseguir anotar ouvindo.
    fun segredo(segredo: String): String = segredo.chunked(4).joinToString(", ") { soletrar(it) }

    // Resumo de uma série de medições, no lugar do gráfico desenhado.
    fun resumoSerie(nome: String, unidade: String, valores: List<Double>): String {
        if (valores.isEmpty()) return "Gráfico de $nome sem medições."
        fun f(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v).replace('.', ',')
        val tendencia = when {
            valores.size < 2 -> "estável"
            valores.last() > valores.first() -> "subindo"
            valores.last() < valores.first() -> "descendo"
            else -> "estável"
        }
        return "Gráfico de $nome com ${valores.size} medições. Mais recente ${f(valores.last())} $unidade. " +
            "Menor ${f(valores.min())}, maior ${f(valores.max())}. Tendência: $tendencia."
    }

    // "124/80 mmHg · você" -> "124 por 80 milímetros de mercúrio, registrado por você".
    fun medida(texto: String): String = texto
        .replace("mmHg", "milímetros de mercúrio")
        .replace("mg/dL", "miligramas por decilitro")
        .replace(Regex("""(\d)/(\d)"""), "$1 por $2")
        .replace(" · você", ", registrado por você")
        .replace(" · ", ", ")
        .replace(Regex("""\bkg\b"""), "quilos")
}
