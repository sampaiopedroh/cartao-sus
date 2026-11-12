package br.edu.cartaosus.navegacao

import br.edu.cartaosus.ui.comum.TelaNomeada
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.cartaosus.CartaoApp
import br.edu.cartaosus.ui.cadastro.CadastroScreen
import br.edu.cartaosus.ui.cadastro.CadastroViewModel
import br.edu.cartaosus.ui.login.LoginScreen
import br.edu.cartaosus.ui.login.LoginViewModel

object Rotas {
    // Fluxo externo
    const val VERIFICANDO = "verificando"
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val PRINCIPAL = "principal"

    // Abas (fluxo interno, ver PrincipalScreen)
    const val INICIO = "inicio"
    const val TOKEN = "token"
    const val CONSULTA = "consulta"
    const val SINAIS = "sinais"
    const val DOCUMENTOS = "documentos"
}

// Fluxo externo: verificar sessão, login, cadastro e a área logada. As abas ficam
// num NavHost próprio (PrincipalScreen), cujo destino inicial é o Início. Assim a aba
// "Início" sempre volta à tela inicial e o botão voltar do sistema se comporta de forma previsível.
@Composable
fun NavegacaoApp() {
    val app = LocalContext.current.applicationContext as CartaoApp
    val navegador = rememberNavController()
    var nome by remember { mutableStateOf(app.sessao.nome ?: "") }

    NavHost(navController = navegador, startDestination = Rotas.VERIFICANDO) {
        composable(Rotas.VERIFICANDO) {
            LaunchedEffect(Unit) {
                if (app.repositorio.sessaoValida()) {
                    nome = app.sessao.nome ?: ""
                    navegador.navigate(Rotas.PRINCIPAL) { popUpTo(Rotas.VERIFICANDO) { inclusive = true } }
                } else {
                    navegador.navigate(Rotas.LOGIN) { popUpTo(Rotas.VERIFICANDO) { inclusive = true } }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(Rotas.LOGIN) {
            val viewModel: LoginViewModel = viewModel(initializer = { LoginViewModel(app.repositorio) })
            TelaNomeada("Entrar no Cartão SUS") { LoginScreen(
                viewModel = viewModel,
                onEntrou = {
                    nome = app.sessao.nome ?: ""
                    navegador.navigate(Rotas.PRINCIPAL) { popUpTo(Rotas.LOGIN) { inclusive = true } }
                },
                onCriarConta = { navegador.navigate(Rotas.CADASTRO) },
            ) }
        }

        composable(Rotas.CADASTRO) {
            val viewModel: CadastroViewModel = viewModel(initializer = { CadastroViewModel(app.repositorio, app.sessao) })
            TelaNomeada("Criar conta") { CadastroScreen(viewModel = viewModel, onVoltar = { navegador.popBackStack() }) }
        }

        composable(Rotas.PRINCIPAL) {
            PrincipalScreen(
                app = app,
                nome = nome,
                onSair = {
                    app.repositorio.sair()
                    navegador.navigate(Rotas.LOGIN) { popUpTo(0) }
                },
            )
        }
    }
}
