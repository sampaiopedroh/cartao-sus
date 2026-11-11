package br.edu.cartaosus.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.LaunchedEffect
import br.edu.cartaosus.data.AlertaResumo
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.ui.Estado
import br.edu.cartaosus.ui.ConteudoEstado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val repositorio: CidadaoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(Estado<List<AlertaResumo>>())
    val estado: StateFlow<Estado<List<AlertaResumo>>> = _estado.asStateFlow()

    init { carregar() }

    fun carregar() {
        _estado.value = Estado(carregando = true)
        viewModelScope.launch {
            _estado.value = try {
                Estado(carregando = false, dados = repositorio.alertas())
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun confirmar(alerta: AlertaResumo) {
        viewModelScope.launch {
            try {
                repositorio.marcarEntregue(alerta.idAlerta)
                carregar()
            } catch (_: Exception) {
                // Mantém a lista; o alerta continua pendente até nova tentativa.
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    nome: String,
    onAbrirToken: () -> Unit,
    onAbrirConsulta: () -> Unit,
    onAbrirSinais: () -> Unit,
) {
    val estado by viewModel.estado.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CartaoDigital(nome)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAbrirToken) { Text("Token") }
            Button(onClick = onAbrirConsulta) { Text("Consulta SUS") }
            Button(onClick = onAbrirSinais) { Text("Sinais") }
        }
        Text("Alertas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ConteudoEstado(
            estado = estado,
            vazio = estado.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhum alerta por enquanto.",
            onTentar = viewModel::carregar,
        ) { alertas ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alertas) { alerta -> CartaoAlerta(alerta, onConfirmar = { viewModel.confirmar(alerta) }) }
            }
        }
    }
}

@Composable
private fun CartaoDigital(nome: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cartão SUS", style = MaterialTheme.typography.labelLarge)
            Text(nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartaoAlerta(alerta: AlertaResumo, onConfirmar: () -> Unit) {
    val critico = alerta.severidade == "CRITICA"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (critico) Color(0xFFFDECEA) else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(alerta.mensagem, style = MaterialTheme.typography.bodyLarge, fontWeight = if (critico) FontWeight.Bold else FontWeight.Normal)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(alerta.criadoEm.take(16).replace('T', ' '), style = MaterialTheme.typography.bodySmall)
                if (alerta.statusEnvio == "PENDENTE") TextButton(onClick = onConfirmar) { Text("Confirmar") }
            }
        }
    }
}
