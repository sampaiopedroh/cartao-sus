package br.edu.cartaosus.ui.sinais

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.MedicaoResumo
import br.edu.cartaosus.data.MedicaoPendente
import br.edu.cartaosus.data.SinalVitalResposta
import br.edu.cartaosus.domain.Grafico
import br.edu.cartaosus.ui.ConteudoEstado
import br.edu.cartaosus.ui.Estado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TipoMedicao(val codigo: String, val rotulo: String)

val TIPOS = listOf(
    TipoMedicao("PRESSAO_SISTOLICA", "Pressão sistólica"),
    TipoMedicao("PRESSAO_DIASTOLICA", "Pressão diastólica"),
    TipoMedicao("GLICEMIA", "Glicemia"),
    TipoMedicao("PESO", "Peso"),
)

sealed class ResultadoRegistro {
    data class Enviado(val resposta: SinalVitalResposta) : ResultadoRegistro()
    data object Pendente : ResultadoRegistro()
    data class Erro(val mensagem: String) : ResultadoRegistro()
}

class SinaisViewModel(private val repositorio: CidadaoRepository) : ViewModel() {
    private val _tipo = MutableStateFlow(TIPOS.first())
    val tipo: StateFlow<TipoMedicao> = _tipo.asStateFlow()

    private val _historico = MutableStateFlow(Estado<List<MedicaoResumo>>())
    val historico: StateFlow<Estado<List<MedicaoResumo>>> = _historico.asStateFlow()

    private val _resultado = MutableStateFlow<ResultadoRegistro?>(null)
    val resultado: StateFlow<ResultadoRegistro?> = _resultado.asStateFlow()

    val pendentes = repositorio.pendentes()

    init {
        carregar()
        sincronizar()
    }

    fun escolherTipo(t: TipoMedicao) {
        _tipo.value = t
        carregar()
    }

    fun carregar() {
        _historico.value = Estado(carregando = true)
        viewModelScope.launch {
            _historico.value = try {
                Estado(carregando = false, dados = repositorio.sinais(_tipo.value.codigo).medicoes)
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun sincronizar() {
        viewModelScope.launch {
            val enviados = repositorio.sincronizarPendentes()
            if (enviados > 0) carregar()
        }
    }

    fun registrar(valorTexto: String) {
        val valor = valorTexto.replace(',', '.').toDoubleOrNull()
        if (valor == null || valor <= 0) {
            _resultado.value = ResultadoRegistro.Erro("Informe um valor maior que zero.")
            return
        }
        viewModelScope.launch {
            _resultado.value = try {
                when (val r = repositorio.registrarMedicao(_tipo.value.codigo, valor)) {
                    is CidadaoRepository.Resultado.Enviado -> ResultadoRegistro.Enviado(r.resposta)
                    CidadaoRepository.Resultado.Pendente -> ResultadoRegistro.Pendente
                }
            } catch (e: Exception) {
                ResultadoRegistro.Erro(Erros.mensagem(e))
            }
            carregar()
        }
    }

    fun limparResultado() { _resultado.value = null }
}

@Composable
fun SinaisScreen(viewModel: SinaisViewModel) {
    val tipo by viewModel.tipo.collectAsState()
    val historico by viewModel.historico.collectAsState()
    val resultado by viewModel.resultado.collectAsState()
    val pendentes by viewModel.pendentes.collectAsState(initial = emptyList())
    var valor by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Sinais vitais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TIPOS.forEach { t ->
                FilterChip(selected = tipo == t, onClick = { viewModel.escolherTipo(t) }, label = { Text(t.rotulo) })
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor (${tipo.rotulo.lowercase()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { viewModel.registrar(valor) }, modifier = Modifier.fillMaxWidth()) { Text("Registrar medição") }
            }
        }

        when (val r = resultado) {
            is ResultadoRegistro.Enviado -> {
                if (r.resposta.alertaDisparado) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Alerta crítico", fontWeight = FontWeight.Bold)
                            Text(r.resposta.alerta?.mensagem ?: "Procure a UPA mais próxima.")
                            TextButton(onClick = viewModel::limparResultado) { Text("Entendi") }
                        }
                    }
                } else {
                    Text("Medição registrada.")
                }
            }
            ResultadoRegistro.Pendente -> Text("Salvo no aparelho. Será enviado quando houver internet.")
            is ResultadoRegistro.Erro -> Text(r.mensagem)
            null -> Unit
        }

        if (pendentes.isNotEmpty()) {
            Text("Aguardando envio: ${pendentes.size} medição(ões)", style = MaterialTheme.typography.bodySmall)
        }

        Text("Histórico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ConteudoEstado(
            estado = historico,
            vazio = historico.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhuma medição registrada.",
            onTentar = viewModel::carregar,
        ) { lista ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!Grafico.poucosDados(lista.size)) {
                    Grafico(valores = lista.map { it.valor })
                } else {
                    Text("Registre mais medições para ver o gráfico.")
                }
                lista.takeLast(10).reversed().forEach { m ->
                    Text("${m.dataHora.take(16).replace('T', ' ')} · ${m.valor} · ${m.origem}")
                }
            }
        }
    }
}

// Gráfico de linha desenhado com Canvas (sem biblioteca de gráficos; SDD 7.2).
@Composable
fun Grafico(valores: List<Double>) {
    val pontos = Grafico.normalizar(valores)
    val cor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        if (pontos.size < 2) return@Canvas
        val passo = size.width / (pontos.size - 1)
        val caminho = Path()
        pontos.forEachIndexed { i, p ->
            val x = i * passo
            val y = size.height - p * size.height
            if (i == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
        }
        drawPath(caminho, color = cor, style = Stroke(width = 4f))
        pontos.forEachIndexed { i, p ->
            drawCircle(color = cor, radius = 6f, center = Offset(i * passo, size.height - p * size.height))
        }
    }
}
