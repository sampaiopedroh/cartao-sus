package br.edu.cartaosus.navegacao

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.cartaosus.CartaoApp
import br.edu.cartaosus.ui.consulta.ConsultaScreen
import br.edu.cartaosus.ui.consulta.ConsultaViewModel
import br.edu.cartaosus.ui.dashboard.DashboardScreen
import br.edu.cartaosus.ui.dashboard.DashboardViewModel
import br.edu.cartaosus.ui.sinais.SinaisScreen
import br.edu.cartaosus.ui.sinais.SinaisViewModel
import br.edu.cartaosus.ui.token.TokenScreen
import br.edu.cartaosus.ui.token.TokenViewModel

private data class Aba(val rota: String, val rotulo: String, val titulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba(Rotas.INICIO, "Início", "Cartão SUS", Icons.Filled.Home),
    Aba(Rotas.TOKEN, "Atendimento", "Código de atendimento", Icons.Filled.Lock),
    Aba(Rotas.CONSULTA, "Consultar", "Remédios e unidades", Icons.Filled.Search),
    Aba(Rotas.SINAIS, "Saúde", "Meus sinais de saúde", Icons.Filled.Favorite),
)

// Troca de aba: sempre desempilha até o Início (destino inicial) e restaura o estado da aba.
private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalScreen(app: CartaoApp, nome: String, onSair: () -> Unit) {
    val navegador = rememberNavController()
    val entrada by navegador.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route ?: Rotas.INICIO
    val abaAtual = ABAS.firstOrNull { it.rota == rotaAtual } ?: ABAS.first()
    var confirmandoSaida by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(abaAtual.titulo, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { confirmandoSaida = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair da conta")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                ABAS.forEach { aba ->
                    NavigationBarItem(
                        selected = rotaAtual == aba.rota,
                        onClick = { navegador.irParaAba(aba.rota) },
                        icon = { Icon(aba.icone, contentDescription = null) },
                        label = { Text(aba.rotulo, style = MaterialTheme.typography.labelMedium) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { espaco ->
        NavHost(
            navController = navegador,
            startDestination = Rotas.INICIO,
            modifier = Modifier.padding(espaco),
        ) {
            composable(Rotas.INICIO) {
                val viewModel: DashboardViewModel = viewModel(initializer = { DashboardViewModel(app.cidadao) })
                DashboardScreen(
                    viewModel = viewModel,
                    nome = nome,
                    onAbrir = { navegador.irParaAba(it) },
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
                SinaisScreen(viewModel = viewModel, onVerUnidades = { navegador.irParaAba(Rotas.CONSULTA) })
            }
        }
    }

    if (confirmandoSaida) {
        AlertDialog(
            onDismissRequest = { confirmandoSaida = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você vai precisar entrar de novo com e-mail e senha.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { confirmandoSaida = false; onSair() }) { Text("Sair", style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = { confirmandoSaida = false }) { Text("Ficar", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}
