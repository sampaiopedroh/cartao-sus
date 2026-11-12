package br.edu.cartaosus.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.AuthRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.LoginResponse
import br.edu.cartaosus.ui.Acao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormLogin(val login: String = "", val senha: String = "")

class LoginViewModel(private val repositorio: AuthRepository) : ViewModel() {
    private val _form = MutableStateFlow(FormLogin())
    val form: StateFlow<FormLogin> = _form.asStateFlow()

    private val _acao = MutableStateFlow<Acao<LoginResponse>>(Acao.Ociosa)
    val acao: StateFlow<Acao<LoginResponse>> = _acao.asStateFlow()

    // Os campos ficam no ViewModel: sobrevivem à rotação da tela.
    // Ao editar, some a mensagem de erro anterior.
    fun alterarLogin(valor: String) { limparErro(); _form.update { it.copy(login = valor) } }
    fun alterarSenha(valor: String) { limparErro(); _form.update { it.copy(senha = valor) } }

    private fun limparErro() {
        if (_acao.value is Acao.Erro) _acao.value = Acao.Ociosa
    }

    fun entrar() {
        if (_acao.value is Acao.Carregando) return
        val f0 = _form.value
        // Validação antes de chamar a rede: mensagem clara e imediata.
        val problema = br.edu.cartaosus.domain.Validacao.email(f0.login) ?: if (f0.senha.isEmpty()) "Informe a senha." else null
        if (problema != null) {
            _acao.value = Acao.Erro(problema)
            return
        }
        _acao.value = Acao.Carregando
        viewModelScope.launch {
            val f = _form.value
            _acao.value = try {
                Acao.Sucesso(repositorio.entrar(f.login.trim(), f.senha))
            } catch (erro: Exception) {
                Acao.Erro(Erros.mensagem(erro))
            }
        }
    }

    // Depois de navegar, zera o estado para não repetir a navegação.
    fun consumirAcao() {
        _acao.value = Acao.Ociosa
    }

    private inline fun MutableStateFlow<FormLogin>.update(transformacao: (FormLogin) -> FormLogin) {
        value = transformacao(value)
    }
}
