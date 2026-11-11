package br.edu.cartaosus.data

// Concentra as chamadas de acesso e a gravação da sessão (SDD 5.4: camada de dados).
class AuthRepository(private val api: SusApi, private val sessao: SessionStore) {

    suspend fun entrar(login: String, senha: String): LoginResponse {
        val resposta = api.login(LoginRequest(login, senha))
        sessao.token = resposta.token
        sessao.nome = resposta.nome
        sessao.idCidadao = resposta.idCidadao ?: -1L
        return resposta
    }

    suspend fun cadastrar(pedido: CadastroRequest): CadastroResponse = api.cadastro(pedido)

    // Confirma com a API se o token salvo ainda vale.
    suspend fun sessaoValida(): Boolean {
        if (sessao.token.isNullOrBlank()) return false
        return try {
            api.me()
            true
        } catch (_: Exception) {
            sessao.limpar()
            false
        }
    }

    fun sair() = sessao.limpar()

    fun sessao(): SessionStore = sessao

    fun nomeSalvo(): String? = sessao.nome
}
