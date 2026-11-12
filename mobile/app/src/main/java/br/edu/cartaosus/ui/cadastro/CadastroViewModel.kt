package br.edu.cartaosus.ui.cadastro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.AuthRepository
import br.edu.cartaosus.data.CadastroRequest
import br.edu.cartaosus.data.CadastroResponse
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.SessionStore
import br.edu.cartaosus.domain.Validacao
import br.edu.cartaosus.ui.Acao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormCadastro(
    val login: String = "",
    val senha: String = "",
    val cpf: String = "",
    val cns: String = "",
    val nome: String = "",
    val nascimento: String = "",
)

// Erros de validação por campo (SDD 7.2: erro por campo, não só no topo).
data class ErrosCampos(val mapa: Map<String, String> = emptyMap())

class CadastroViewModel(private val repositorio: AuthRepository, private val sessao: SessionStore) : ViewModel() {
    private val _salvo = MutableStateFlow(false)
    val salvo: StateFlow<Boolean> = _salvo.asStateFlow()

    // Guarda o segredo TOTP no aparelho (decisão 1 do plano F8).
    fun salvarNoAparelho(resposta: CadastroResponse) {
        sessao.salvarSegredo(resposta.idCidadao, resposta.segredoTotp)
        _salvo.value = true
    }

    private val _form = MutableStateFlow(FormCadastro())
    val form: StateFlow<FormCadastro> = _form.asStateFlow()

    private val _erros = MutableStateFlow(ErrosCampos())
    val erros: StateFlow<ErrosCampos> = _erros.asStateFlow()

    private val _acao = MutableStateFlow<Acao<CadastroResponse>>(Acao.Ociosa)
    val acao: StateFlow<Acao<CadastroResponse>> = _acao.asStateFlow()

    fun alterar(campo: String, valor: String) {
        val chave = when (campo) { "cns" -> "numeroCns"; "nome" -> "nomeCompleto"; "nascimento" -> "dataNascimento"; else -> campo }
        if (_erros.value.mapa.containsKey(chave)) _erros.value = ErrosCampos(_erros.value.mapa - chave)
        _form.value = when (campo) {
            "login" -> _form.value.copy(login = valor)
            "senha" -> _form.value.copy(senha = valor)
            "cpf" -> _form.value.copy(cpf = valor.filter { it.isDigit() }.take(11))
            "cns" -> _form.value.copy(cns = valor.filter { it.isDigit() }.take(15))
            "nome" -> _form.value.copy(nome = valor)
            "nascimento" -> _form.value.copy(nascimento = valor.filter { it.isDigit() }.take(8))
            else -> _form.value
        }
    }

    fun validar(): Map<String, String> {
        val f = _form.value
        val mapa = listOfNotNull(
            Validacao.email(f.login)?.let { "login" to it },
            Validacao.senha(f.senha)?.let { "senha" to it },
            Validacao.cpf(f.cpf)?.let { "cpf" to it },
            Validacao.cns(f.cns)?.let { "numeroCns" to it },
            Validacao.nome(f.nome)?.let { "nomeCompleto" to it },
            Validacao.nascimentoDigitos(f.nascimento)?.let { "dataNascimento" to it },
        ).toMap()
        _erros.value = ErrosCampos(mapa)
        return mapa
    }

    fun cadastrar() {
        if (_acao.value is Acao.Carregando) return
        if (validar().isNotEmpty()) return
        _acao.value = Acao.Carregando
        viewModelScope.launch {
            val f = _form.value
            _acao.value = try {
                Acao.Sucesso(repositorio.cadastrar(CadastroRequest(
                    login = f.login.trim(),
                    senha = f.senha,
                    cpf = f.cpf,
                    numeroCns = f.cns,
                    nomeCompleto = f.nome.trim(),
                    dataNascimento = Validacao.paraIso(f.nascimento),
                )))
            } catch (erro: Exception) {
                _erros.value = ErrosCampos(Erros.campos(erro).mapKeys { it.key })
                Acao.Erro(Erros.mensagem(erro), Erros.campos(erro))
            }
        }
    }

    fun consumirAcao() {
        _acao.value = Acao.Ociosa
    }
}
