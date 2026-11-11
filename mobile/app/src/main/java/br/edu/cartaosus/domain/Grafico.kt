package br.edu.cartaosus.domain

// Pontos do gráfico de linha normalizados para [0, 1] (SDD 7.2, tela 6).
object Grafico {
    const val MINIMO_PONTOS = 2

    fun normalizar(valores: List<Double>): List<Float> {
        if (valores.isEmpty()) return emptyList()
        val menor = valores.min()
        val maior = valores.max()
        if (maior == menor) return valores.map { 0.5f }
        return valores.map { ((it - menor) / (maior - menor)).toFloat() }
    }

    fun poucosDados(total: Int): Boolean = total < MINIMO_PONTOS
}
