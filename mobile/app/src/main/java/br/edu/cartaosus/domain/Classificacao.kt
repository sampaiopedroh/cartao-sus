package br.edu.cartaosus.domain

// Linguagem simples para quem não conhece os limites clínicos. Os limiares de crise
// são os mesmos da API (SDD 9.4): sistólica > 180 ou diastólica > 110; glicemia >= 300.
enum class Nivel { NORMAL, ATENCAO, CRITICO }

data class Avaliacao(val nivel: Nivel, val mensagem: String)

object Classificacao {
    fun pressao(sistolica: Double, diastolica: Double): Avaliacao = when {
        sistolica > 180 || diastolica > 110 ->
            Avaliacao(Nivel.CRITICO, "Pressão muito alta. Procure a UPA mais próxima.")
        sistolica >= 140 || diastolica >= 90 ->
            Avaliacao(Nivel.ATENCAO, "Pressão elevada. Converse com seu médico.")
        else -> Avaliacao(Nivel.NORMAL, "Pressão normal.")
    }

    fun glicemia(valor: Double): Avaliacao = when {
        valor >= 300 -> Avaliacao(Nivel.CRITICO, "Glicemia muito alta. Procure atendimento de saúde.")
        valor >= 180 -> Avaliacao(Nivel.ATENCAO, "Glicemia elevada. Converse com seu médico.")
        else -> Avaliacao(Nivel.NORMAL, "Glicemia dentro do esperado.")
    }
}
