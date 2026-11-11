package br.edu.cartaosus

import android.app.Application
import br.edu.cartaosus.data.AuthRepository
import br.edu.cartaosus.data.ClienteApi
import br.edu.cartaosus.data.SessionStore

// Ponto único de criação das dependências do app (sem framework de injeção).
class CartaoApp : Application() {
    val repositorio: AuthRepository by lazy {
        val sessao = SessionStore(this)
        AuthRepository(ClienteApi.criar(sessao), sessao)
    }
}
