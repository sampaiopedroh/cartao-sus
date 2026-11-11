package br.edu.cartaosus.navegacao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.cartaosus.CartaoApp
import br.edu.cartaosus.ui.cadastro.CadastroScreen
import br.edu.cartaosus.ui.cadastro.CadastroViewModel
import br.edu.cartaosus.ui.consulta.ConsultaScreen
import br.edu.cartaosus.ui.consulta.ConsultaViewModel
import br.edu.cartaosus.ui.dashboard.DashboardScreen
import br.edu.cartaosus.ui.dashboard.DashboardViewModel
import br.edu.cartaosus.ui.login.LoginScreen
import br.edu.cartaosus.ui.login.LoginViewModel
import br.edu.cartaosus.ui.sinais.SinaisScreen
import br.edu.cartaosus.ui.sinais.SinaisViewModel
import br.edu.cartaosus.ui.token.TokenScreen
import br.edu.cartaosus.ui.token.TokenViewModel

object Rotas {
    const val VERIFICANDO = "verificando"
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val INICIO = "inicio"
    const val TOKEN = "token"
    const val CONSULTA = "consulta"
    const val SINAIS = "sinais"
}

private val ABAS = listOf(
    Rotas.INICIO to "Início",
    Rotas.TOKEN to "Token",
    Rotas.CONSULTA to "Consulta",
    Rotas.SINAIS to "Sinais",
)

@Composable
fun NavegacaoApp() {
    val app = LocalContext.current.applicationContext as CartaoApp
    val navegador = rememberNavController()
    val entrada by navegador.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val logado = rotaAtual in ABAS.map { it.first }
    var nome by remember { mutableStateOf(app.sessao.nome ?: "") }

    Scaffold(
        bottomBar = {
            if (logado) {
                NavigationBar {
                    ABAS.forEach { (rota, rotulo) ->
                        NavigationBarItem(
                            selected = rotaAtual == rota,
                            onClick = {
                                navegador.navigate(rota) {
                                    popUpTo(Rotas.INICIO) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(rotulo.take(1)) },
                            label = { Text(rotulo) },
                        )
                    }
                }
            }
        },
    ) { espaco ->
        NavHost(
            navController = navegador,
            startDestination = Rotas.VERIFICANDO,
            modifier = Modifier.padding(espaco),
        ) {
            composable(Rotas.VERIFICANDO) {
                LaunchedEffect(Unit) {
                    if (app.repositorio.sessaoValida()) {
                        nome = app.sessao.nome ?: ""
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
                val viewModel: LoginViewModel = viewModel(initializer = { LoginViewModel(app.repositorio) })
                LoginScreen(
                    viewModel = viewModel,
                    onEntrou = {
                        nome = app.sessao.nome ?: ""
                        navegador.navigate(Rotas.INICIO) { popUpTo(Rotas.LOGIN) { inclusive = true } }
                    },
                    onCriarConta = { navegador.navigate(Rotas.CADASTRO) },
                )
            }

            composable(Rotas.CADASTRO) {
                val viewModel: CadastroViewModel = viewModel(initializer = { CadastroViewModel(app.repositorio, app.sessao) })
                CadastroScreen(viewModel = viewModel, onVoltar = { navegador.popBackStack() })
            }

            composable(Rotas.INICIO) {
                val viewModel: DashboardViewModel = viewModel(initializer = { DashboardViewModel(app.cidadao) })
                DashboardScreen(
                    viewModel = viewModel,
                    nome = nome,
                    onAbrirToken = { navegador.navigate(Rotas.TOKEN) { launchSingleTop = true } },
                    onAbrirConsulta = { navegador.navigate(Rotas.CONSULTA) { launchSingleTop = true } },
                    onAbrirSinais = { navegador.navigate(Rotas.SINAIS) { launchSingleTop = true } },
                    onSair = {
                        app.repositorio.sair()
                        navegador.navigate(Rotas.LOGIN) { popUpTo(0) }
                    },
                )
            }

            composable(Rotas.TOKEN) {
                val viewModel: TokenViewModel = viewModel(initializer = { TokenViewModel(app.sessao, app.cidadao) })
                TokenScreen(viewModel = viewModel)
            }

            composable(Rotas.CONSULTA) {
                val viewModel: ConsultaViewModel = viewModel(initializer = { ConsultaViewModel(app.sus) })
                ConsultaScreen(viewModel = viewModel)
            }

            composable(Rotas.SINAIS) {
                val viewModel: SinaisViewModel = viewModel(initializer = { SinaisViewModel(app.cidadao) })
                SinaisScreen(viewModel = viewModel)
            }
        }
    }
}
