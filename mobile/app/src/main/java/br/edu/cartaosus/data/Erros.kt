package br.edu.cartaosus.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

// Traduz falhas de rede e o envelope de erro da API em texto para o usuário (SDD 7.3 e 7.6).
object Erros {
    const val SEM_CONEXAO = "Sem conexão. Verifique sua internet."
    const val CREDENCIAIS = "Credenciais inválidas."
    const val CONFLITO = "Já existe conta com este CPF ou CNS."
    const val SESSAO_EXPIRADA = "Sessão expirada, entre novamente."

    fun mensagem(erro: Throwable): String = when (erro) {
        is IOException -> SEM_CONEXAO
        is HttpException -> mensagemHttp(erro)
        else -> "Algo deu errado. Tente novamente."
    }

    // Campo por campo, quando a API devolve `campos` (validação).
    fun campos(erro: Throwable): Map<String, String> =
        (erro as? HttpException)?.let { lerEnvelope(it)?.campos }.orEmpty()

    private fun mensagemHttp(erro: HttpException): String {
        val envelope = lerEnvelope(erro)
        return when (erro.code()) {
            401 -> if (envelope?.erro == "NAO_AUTORIZADO") CREDENCIAIS else SESSAO_EXPIRADA
            409 -> envelope?.mensagem ?: CONFLITO
            else -> envelope?.mensagem ?: "Erro ${erro.code()} do servidor."
        }
    }

    private fun lerEnvelope(erro: HttpException): ErroResposta? = try {
        erro.response()?.errorBody()?.string()?.let { Gson().fromJson(it, ErroResposta::class.java) }
    } catch (_: Exception) {
        null
    }
}
