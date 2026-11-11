package br.edu.cartaosus.navegacao

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
import br.edu.cartaosus.ui.inicio.InicioScreen
import br.edu.cartaosus.ui.login.LoginScreen
import br.edu.cartaosus.ui.login.LoginViewModel

object Rotas {
    const val VERIFICANDO = "verificando"
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val INICIO = "inicio"
}

@Composable
fun NavegacaoApp() {
    val repositorio = (LocalContext.current.applicationContext as CartaoApp).repositorio
    val navegador = rememberNavController()
    var nome by remember { mutableStateOf(repositorio.nomeSalvo() ?: "") }

    NavHost(navController = navegador, startDestination = Rotas.VERIFICANDO) {
        composable(Rotas.VERIFICANDO) {
            // Ao abrir, confere se o token salvo ainda vale (SDD 7.6).
            LaunchedEffect(Unit) {
                if (repositorio.sessaoValida()) {
                    nome = repositorio.nomeSalvo() ?: ""
                    navegador.navigate(Rotas.INICIO) { popUpTo(Rotas.VERIFICANDO) { inclusive = true } }
                } else {
                    navegador.navigate(Rotas.LOGIN) { popUpTo(Rotas.VERIFICANDO) { inclusive = true } }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(Rotas.LOGIN) {
            val viewModel: LoginViewModel = viewModel(initializer = { LoginViewModel(repositorio) })
            LoginScreen(
                viewModel = viewModel,
                onEntrou = {
                    nome = repositorio.nomeSalvo() ?: ""
                    navegador.navigate(Rotas.INICIO) { popUpTo(Rotas.LOGIN) { inclusive = true } }
                },
                onCriarConta = { navegador.navigate(Rotas.CADASTRO) },
            )
        }

        composable(Rotas.CADASTRO) {
            val viewModel: CadastroViewModel = viewModel(initializer = { CadastroViewModel(repositorio) })
            CadastroScreen(viewModel = viewModel, onVoltar = { navegador.popBackStack() })
        }

        composable(Rotas.INICIO) {
            InicioScreen(
                nome = nome,
                onSair = {
                    repositorio.sair()
                    navegador.navigate(Rotas.LOGIN) { popUpTo(0) }
                },
            )
        }
    }
}
