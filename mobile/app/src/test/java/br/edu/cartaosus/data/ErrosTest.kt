package br.edu.cartaosus.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErrosTest {

    private fun httpErro(codigo: Int, corpo: String): HttpException =
        HttpException(Response.error<Any>(codigo, corpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun falhaDeRedeViraSemConexao() =
        assertEquals(Erros.SEM_CONEXAO, Erros.mensagem(IOException("timeout")))

    @Test
    fun senhaErradaVirarCredenciaisInvalidas() {
        val corpo = """{"status":401,"erro":"NAO_AUTORIZADO","mensagem":"Credenciais inválidas.","campos":{}}"""
        assertEquals(Erros.CREDENCIAIS, Erros.mensagem(httpErro(401, corpo)))
    }

    @Test
    fun tokenInvalidoNoLoginEhTratadoComoCredenciais() {
        // O 401 de token expirado é tratado pelo AuthRepository.sessaoValida, que limpa a sessão sem mostrar mensagem.
        val corpo = """{"status":401,"erro":"NAO_AUTORIZADO","mensagem":"Token ausente, inválido ou expirado.","campos":{}}"""
        assertEquals(Erros.CREDENCIAIS, Erros.mensagem(httpErro(401, corpo)))
    }

    @Test
    fun conflitoUsaAMensagemDaApi() {
        val corpo = """{"status":409,"erro":"CONFLITO","mensagem":"Já existe conta com este CPF, CNS ou login.","campos":{}}"""
        assertEquals("Já existe conta com este CPF, CNS ou login.", Erros.mensagem(httpErro(409, corpo)))
    }

    @Test
    fun validacaoDevolveCamposParaCadaCampo() {
        val corpo = """{"status":400,"erro":"VALIDACAO","mensagem":"senha deve ter pelo menos 8 caracteres","campos":{"senha":"senha deve ter pelo menos 8 caracteres"}}"""
        assertEquals(mapOf("senha" to "senha deve ter pelo menos 8 caracteres"), Erros.campos(httpErro(400, corpo)))
    }

    @Test
    fun respostaDeLoginEhLidaPeloGson() {
        val json = """{"token":"abc","tipo":"Bearer","expiraEmMinutos":30,"perfil":"CIDADAO","idCidadao":4,"nome":"Maria"}"""
        val resposta = com.google.gson.Gson().fromJson(json, LoginResponse::class.java)
        assertEquals("abc", resposta.token)
        assertEquals(4L, resposta.idCidadao)
        assertEquals(30, resposta.expiraEmMinutos)
    }
}
