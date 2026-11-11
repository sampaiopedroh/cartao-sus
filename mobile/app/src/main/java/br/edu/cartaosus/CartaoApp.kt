package br.edu.cartaosus

import android.app.Application
import br.edu.cartaosus.data.AppDatabase
import br.edu.cartaosus.data.AuthRepository
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.ClienteApi
import br.edu.cartaosus.data.SessionStore
import br.edu.cartaosus.data.SusApi
import br.edu.cartaosus.data.SusRepository

// Ponto único de criação das dependências do app (sem framework de injeção).
class CartaoApp : Application() {
    val sessao: SessionStore by lazy { SessionStore(this) }
    val api: SusApi by lazy { ClienteApi.criar(sessao) }
    val banco: AppDatabase by lazy { AppDatabase.criar(this) }

    val repositorio: AuthRepository by lazy { AuthRepository(api, sessao) }
    val cidadao: CidadaoRepository by lazy { CidadaoRepository(api, sessao, banco) }
    val sus: SusRepository by lazy { SusRepository(api) }
}
