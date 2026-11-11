package br.edu.cartaosus.data

// DTOs espelhando os contratos da API (SDD 5.3). Nomes em camelCase, como no JSON.

data class LoginRequest(val login: String, val senha: String)

data class LoginResponse(
    val token: String,
    val tipo: String,
    val expiraEmMinutos: Int,
    val perfil: String,
    val idCidadao: Long?,
    val nome: String,
)

data class CadastroRequest(
    val login: String,
    val senha: String,
    val cpf: String,
    val numeroCns: String,
    val nomeCompleto: String,
    val dataNascimento: String,
)

data class CadastroResponse(val idCidadao: Long, val segredoTotp: String, val mensagem: String)

data class MeResponse(val idUsuario: Long, val login: String, val perfil: String, val idCidadao: Long?)

// Envelope de erro padrão da API (SDD 5.1).
data class ErroResposta(
    val timestamp: String? = null,
    val status: Int? = null,
    val erro: String? = null,
    val mensagem: String? = null,
    val campos: Map<String, String>? = null,
)
