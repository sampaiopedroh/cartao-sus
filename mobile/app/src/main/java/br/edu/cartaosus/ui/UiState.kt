package br.edu.cartaosus.ui

// Estados de uma ação de rede (SDD 7.1): ocioso, carregando, sucesso e erro.
sealed class Acao<out T> {
    data object Ociosa : Acao<Nothing>()
    data object Carregando : Acao<Nothing>()
    data class Sucesso<T>(val valor: T) : Acao<T>()
    data class Erro(val mensagem: String, val campos: Map<String, String> = emptyMap()) : Acao<Nothing>()
}
