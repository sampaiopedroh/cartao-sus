package br.edu.cartaosus.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.AlertaResumo
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.navegacao.Rotas
import br.edu.cartaosus.ui.Estado
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoTexto
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.EstadoCarregando
import br.edu.cartaosus.ui.comum.EstadoErro
import br.edu.cartaosus.ui.comum.TomAviso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DadosInicio(val cns: String?, val alertas: List<AlertaResumo>)

class DashboardViewModel(private val repositorio: CidadaoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(Estado<DadosInicio>())
    val estado: StateFlow<Estado<DadosInicio>> = _estado.asStateFlow()

    init { carregar() }

    // silencioso: atualiza sem trocar a tela por um indicador de carregamento.
    fun carregar(silencioso: Boolean = false) {
        if (!silencioso || _estado.value.dados == null) _estado.value = Estado(carregando = true)
        viewModelScope.launch {
            _estado.value = try {
                val cns = try { repositorio.prontuario().cns } catch (e: java.io.IOException) { throw e } catch (_: Exception) { null }
                Estado(carregando = false, dados = DadosInicio(cns, repositorio.alertas()))
            } catch (e: Exception) {
                if (silencioso && _estado.value.dados != null) _estado.value.copy(carregando = false)
                else Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun confirmar(alerta: AlertaResumo) {
        viewModelScope.launch {
            try {
                repositorio.marcarEntregue(alerta.idAlerta)
                carregar(silencioso = true)
            } catch (_: Exception) {
                // O alerta continua pendente; o usuário pode tocar de novo.
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, nome: String, onAbrir: (String) -> Unit) {
    val estado by viewModel.estado.collectAsState()
    var ajuda by remember { mutableStateOf(false) }

    // Ao voltar para o Início, atualiza os alertas sem piscar.
    LaunchedEffect(Unit) { viewModel.carregar(silencioso = true) }

    when {
        estado.carregando && estado.dados == null -> EstadoCarregando("Buscando suas informações...")
        estado.erro != null -> EstadoErro(estado.erro!!, onTentar = { viewModel.carregar() })
        else -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val dados = estado.dados!!
            Text("Olá, ${primeiroNome(nome)}!", style = MaterialTheme.typography.headlineMedium)

            CartaoDigital(nome = nome, cns = dados.cns)

            Text("O que você quer fazer?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Atalho("Meu código de atendimento", "Mostrar no balcão", Icons.Filled.Lock, Modifier.weight(1f)) { onAbrir(Rotas.TOKEN) }
                Atalho("Procurar remédios e unidades", "Ver o que tem perto", Icons.Filled.Search, Modifier.weight(1f)) { onAbrir(Rotas.CONSULTA) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Atalho("Meus sinais de saúde", "Pressão, glicemia, peso", Icons.Filled.Favorite, Modifier.weight(1f)) { onAbrir(Rotas.SINAIS) }
                Atalho("Meus documentos", "RG, laudos, receitas", Icons.AutoMirrored.Filled.List, Modifier.weight(1f)) { onAbrir(Rotas.DOCUMENTOS) }
            }
            BotaoTexto("Como funciona o Cartão SUS?", onClick = { ajuda = true })

            Text("Avisos", style = MaterialTheme.typography.titleMedium)
            if (dados.alertas.isEmpty()) {
                Aviso("Tudo certo! Você não tem avisos no momento.", TomAviso.SUCESSO)
            } else {
                dados.alertas.forEach { alerta ->
                    CartaoAviso(alerta, onConfirmar = { viewModel.confirmar(alerta) }, onVerUnidades = { onAbrir(Rotas.CONSULTA) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (ajuda) {
        AlertDialog(
            onDismissRequest = { ajuda = false },
            title = { Text("Como funciona") },
            text = {
                Text(
                    "1. No balcão do posto, abra \"Meu código de atendimento\" e leia o código para o atendente.\n\n" +
                        "2. O atendente só vê seus dados se você mostrar o código. Ele muda a cada 30 segundos.\n\n" +
                        "3. Em \"Procurar remédios e unidades\", veja o que tem perto de você antes de sair de casa.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { TextButton(onClick = { ajuda = false }) { Text("Entendi", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

private fun primeiroNome(nome: String) = nome.trim().substringBefore(' ').ifBlank { "tudo bem" }

@Composable
private fun CartaoDigital(nome: String, cns: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                Text("CARTÃO SUS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
            Text(nome, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
            Column {
                Text("Número do cartão", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    cns?.let { formatarCns(it) } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

internal fun formatarCns(cns: String): String = cns.chunked(3).take(1).joinToString("") + " " +
    cns.drop(3).chunked(4).joinToString(" ")

@Composable
private fun Atalho(titulo: String, dica: String, icone: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(14.dp).height(128.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(dica, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CartaoAviso(alerta: AlertaResumo, onConfirmar: () -> Unit, onVerUnidades: () -> Unit) {
    val critico = alerta.severidade == "CRITICA"
    val pendente = alerta.statusEnvio == "PENDENTE"
    Aviso(
        texto = alerta.mensagem + "\n" + dataAmigavel(alerta.criadoEm),
        tom = if (critico) TomAviso.ERRO else TomAviso.ATENCAO,
        acao = if (pendente) ({
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alerta.tipo == "PRESSAO_CRITICA" || alerta.tipo == "GLICEMIA_CRITICA") {
                    BotaoPrincipal("Ver unidades de saúde", onVerUnidades, icone = Icons.Filled.Search)
                }
                BotaoSecundario("Entendi", onConfirmar, icone = Icons.Filled.CheckCircle)
            }
        }) else null,
    )
}

internal fun dataAmigavel(iso: String): String {
    // "2026-11-09T21:30:00" -> "09/11/2026 às 21:30"
    if (iso.length < 16) return iso
    val d = iso.substring(0, 10).split('-')
    return if (d.size == 3) "${d[2]}/${d[1]}/${d[0]} às ${iso.substring(11, 16)}" else iso
}
