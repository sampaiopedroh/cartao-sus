package br.edu.cartaosus.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

// Regras de formulário (SDD 5.5 e 7.2). Funções puras: testáveis sem emulador.
object Validacao {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun email(valor: String): String? = when {
        valor.isBlank() -> "Informe o e-mail."
        !EMAIL.matches(valor.trim()) -> "E-mail inválido."
        else -> null
    }

    fun senha(valor: String): String? = when {
        valor.isEmpty() -> "Informe a senha."
        valor.length < 8 -> "A senha deve ter pelo menos 8 caracteres."
        else -> null
    }

    fun cpf(valor: String): String? = when {
        !valor.all { it.isDigit() } || valor.length != 11 -> "CPF deve ter 11 dígitos."
        else -> null
    }

    fun cns(valor: String): String? = when {
        !valor.all { it.isDigit() } || valor.length != 15 -> "CNS deve ter 15 dígitos."
        else -> null
    }

    fun nome(valor: String): String? = when {
        valor.trim().length < 3 -> "Informe o nome completo (mínimo 3 caracteres)."
        else -> null
    }

    // Data no formato AAAA-MM-DD, no passado.
    fun nascimento(valor: String, hoje: LocalDate = LocalDate.now()): String? = try {
        val data = LocalDate.parse(valor)
        if (data.isBefore(hoje)) null else "A data deve estar no passado."
    } catch (_: DateTimeParseException) {
        "Use o formato AAAA-MM-DD."
    }
}
