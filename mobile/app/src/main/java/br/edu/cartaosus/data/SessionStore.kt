package br.edu.cartaosus.data

import android.content.Context

// Sessão e segredo TOTP em SharedPreferences privado ao app (SDD 7.4: sem criptografia, limitação registrada).
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sessao", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(CHAVE_TOKEN, null)
        set(valor) = prefs.edit().putString(CHAVE_TOKEN, valor).apply()

    var nome: String?
        get() = prefs.getString(CHAVE_NOME, null)
        set(valor) = prefs.edit().putString(CHAVE_NOME, valor).apply()

    var idCidadao: Long
        get() = prefs.getLong(CHAVE_ID, -1L)
        set(valor) = prefs.edit().putLong(CHAVE_ID, valor).apply()

    // Segredo TOTP guardado junto com o cidadão a que pertence.
    fun salvarSegredo(idCidadao: Long, segredo: String) {
        prefs.edit().putLong(CHAVE_SEGREDO_ID, idCidadao).putString(CHAVE_SEGREDO, segredo).apply()
    }

    fun segredoDoUsuario(): String? {
        val dono = prefs.getLong(CHAVE_SEGREDO_ID, -2L)
        return if (dono == idCidadao) prefs.getString(CHAVE_SEGREDO, null) else null
    }

    fun limpar() {
        // Mantém o segredo TOTP: ele é do aparelho e é reutilizado no próximo login do mesmo cidadão.
        prefs.edit().remove(CHAVE_TOKEN).remove(CHAVE_NOME).remove(CHAVE_ID).apply()
    }

    private companion object {
        const val CHAVE_TOKEN = "token"
        const val CHAVE_NOME = "nome"
        const val CHAVE_ID = "idCidadao"
        const val CHAVE_SEGREDO = "segredoTotp"
        const val CHAVE_SEGREDO_ID = "segredoIdCidadao"
    }
}
