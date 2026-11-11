package br.edu.cartaosus.data

import android.content.Context

// Sessão em SharedPreferences privado ao app (SDD 7.4: limitação registrada, sem criptografia).
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sessao", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(CHAVE_TOKEN, null)
        set(valor) = prefs.edit().putString(CHAVE_TOKEN, valor).apply()

    var nome: String?
        get() = prefs.getString(CHAVE_NOME, null)
        set(valor) = prefs.edit().putString(CHAVE_NOME, valor).apply()

    fun limpar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val CHAVE_TOKEN = "token"
        const val CHAVE_NOME = "nome"
    }
}
