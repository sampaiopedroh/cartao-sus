package br.edu.cartaosus.ui.sinais

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import br.edu.cartaosus.domain.Fala
import br.edu.cartaosus.ui.comum.TituloSecao
import br.edu.cartaosus.ui.comum.titulo
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.MedicaoResumo
import br.edu.cartaosus.domain.Avaliacao
import br.edu.cartaosus.domain.Classificacao
import br.edu.cartaosus.domain.Grafico
import br.edu.cartaosus.domain.Nivel
import br.edu.cartaosus.ui.ConteudoEstado
import br.edu.cartaosus.ui.Estado
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.CartaoSecao
import br.edu.cartaosus.ui.comum.TomAviso
import br.edu.cartaosus.ui.dashboard.dataAmigavel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Grupo(val rotulo: String, val unidade: String) {
    PRESSAO("Pressão", "mmHg"),
    GLICEMIA("Glicemia", "mg/dL"),
    PESO("Peso", "kg"),
}

data class Serie(val nome: String, val valores: List<Double>, val datas: List<String>)
data class Historico(val series: List<Serie>, val linhas: List<Pair<String, String>>)

sealed class Resultado {
    data class Enviado(val avaliacao: Avaliacao?, val alertaDoServidor: Boolean) : Resultado()
    data class Pendente(val avaliacao: Avaliacao?) : Resultado()
    data class Erro(val mensagem: String) : Resultado()
}

class SinaisViewModel(private val repositorio: CidadaoRepository) : ViewModel() {
    private val _grupo = MutableStateFlow(Grupo.PRESSAO)
    val grupo: StateFlow<Grupo> = _grupo.asStateFlow()

    private val _historico = MutableStateFlow(Estado<Historico>())
    val historico: StateFlow<Estado<Historico>> = _historico.asStateFlow()

    private val _resultado = MutableStateFlow<Resultado?>(null)
    val resultado: StateFlow<Resultado?> = _resultado.asStateFlow()

    private val _valorA = MutableStateFlow("")
    val valorA: StateFlow<String> = _valorA.asStateFlow()
    private val _valorB = MutableStateFlow("")
    val valorB: StateFlow<String> = _valorB.asStateFlow()

    private val _salvando = MutableStateFlow(false)
    val salvando: StateFlow<Boolean> = _salvando.asStateFlow()

    val pendentes = repositorio.pendentes()

    init {
        carregar()
        sincronizar()
    }

    fun escolher(g: Grupo) {
        _grupo.value = g
        _resultado.value = null
        _valorA.value = ""
        _valorB.value = ""
        carregar()
    }

    fun alterarA(v: String) { _valorA.value = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6); _resultado.value = null }
    fun alterarB(v: String) { _valorB.value = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6); _resultado.value = null }

    fun carregar() {
        _historico.value = Estado(carregando = true)
        viewModelScope.launch {
            _historico.value = try {
                Estado(carregando = false, dados = montarHistorico(_grupo.value))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    // Atualização periódica em segundo plano: o gráfico acompanha medições feitas em outro aparelho
    // (tensiômetro, API) sem o usuário trocar de tela. Se falhar, mantém o que já está na tela.
    fun atualizarSilencioso() {
        if (_historico.value.carregando) return
        viewModelScope.launch {
            try {
                val g = _grupo.value
                val novo = montarHistorico(g)
                if (g == _grupo.value) _historico.value = Estado(carregando = false, dados = novo)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun montarHistorico(g: Grupo): Historico = when (g) {
        Grupo.PRESSAO -> {
            val s = repositorio.sinais("PRESSAO_SISTOLICA").medicoes
            val d = repositorio.sinais("PRESSAO_DIASTOLICA").medicoes
            val linhas = s.asReversed().map { m ->
                val par = d.firstOrNull { it.dataHora == m.dataHora }
                dataAmigavel(m.dataHora) to "${m.valor.toLong()}/${par?.valor?.toLong() ?: "—"} mmHg · ${origem(m)}"
            }
            Historico(
                listOf(Serie("Máxima (sistólica)", s.map { it.valor }, s.map { it.dataHora }), Serie("Mínima (diastólica)", d.map { it.valor }, d.map { it.dataHora })),
                linhas,
            )
        }
        Grupo.GLICEMIA -> repositorio.sinais("GLICEMIA").medicoes.let { l -> unico("Glicemia", l, "mg/dL") }
        Grupo.PESO -> repositorio.sinais("PESO").medicoes.let { l -> unico("Peso", l, "kg") }
    }

    private fun unico(nome: String, l: List<MedicaoResumo>, un: String) = Historico(
        listOf(Serie(nome, l.map { it.valor }, l.map { it.dataHora })),
        l.asReversed().map { dataAmigavel(it.dataHora) to "${formatar(it.valor)} $un · ${origem(it)}" },
    )

    private fun origem(m: MedicaoResumo) = if (m.origem == "IOT") "aparelho" else "você"
    private fun formatar(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    fun sincronizar() {
        viewModelScope.launch {
            if (repositorio.sincronizarPendentes() > 0) carregar()
        }
    }

    fun registrar() {
        if (_salvando.value) return
        val g = _grupo.value
        val a = _valorA.value.replace(',', '.').toDoubleOrNull()
        val b = _valorB.value.replace(',', '.').toDoubleOrNull()
        if (a == null || a <= 0 || (g == Grupo.PRESSAO && (b == null || b <= 0))) {
            _resultado.value = Resultado.Erro(if (g == Grupo.PRESSAO) "Informe a pressão máxima e a mínima." else "Informe um valor maior que zero.")
            return
        }
        _salvando.value = true
        viewModelScope.launch {
            _resultado.value = try {
                val avaliacao: Avaliacao?
                var servidorAvisou = false
                var offline = false
                when (g) {
                    Grupo.PRESSAO -> {
                        avaliacao = Classificacao.pressao(a, b!!)
                        for ((tipo, v) in listOf("PRESSAO_SISTOLICA" to a, "PRESSAO_DIASTOLICA" to b)) {
                            when (val r = repositorio.registrarMedicao(tipo, v)) {
                                is CidadaoRepository.Resultado.Enviado -> if (r.resposta.alertaDisparado) servidorAvisou = true
                                CidadaoRepository.Resultado.Pendente -> offline = true
                            }
                        }
                    }
                    Grupo.GLICEMIA -> {
                        avaliacao = Classificacao.glicemia(a)
                        when (val r = repositorio.registrarMedicao("GLICEMIA", a)) {
                            is CidadaoRepository.Resultado.Enviado -> servidorAvisou = r.resposta.alertaDisparado
                            CidadaoRepository.Resultado.Pendente -> offline = true
                        }
                    }
                    Grupo.PESO -> {
                        avaliacao = null
                        if (repositorio.registrarMedicao("PESO", a) is CidadaoRepository.Resultado.Pendente) offline = true
                    }
                }
                _valorA.value = ""
                _valorB.value = ""
                if (offline) Resultado.Pendente(avaliacao) else Resultado.Enviado(avaliacao, servidorAvisou)
            } catch (e: Exception) {
                Resultado.Erro(Erros.mensagem(e))
            } finally {
                _salvando.value = false
            }
            carregar()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SinaisScreen(viewModel: SinaisViewModel, onVerUnidades: () -> Unit) {
    val grupo by viewModel.grupo.collectAsState()
    val historico by viewModel.historico.collectAsState()
    val resultado by viewModel.resultado.collectAsState()
    val pendentes by viewModel.pendentes.collectAsState(initial = emptyList())
    val valorA by viewModel.valorA.collectAsState()
    val valorB by viewModel.valorB.collectAsState()
    val salvando by viewModel.salvando.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            viewModel.atualizarSilencioso()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TituloSecao("O que você quer registrar?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Grupo.entries.forEach { g ->
                FilterChip(selected = grupo == g, onClick = { viewModel.escolher(g) }, label = { Text(g.rotulo, style = MaterialTheme.typography.labelLarge) })
            }
        }

        if (pendentes.isNotEmpty()) {
            Aviso(
                "Sem internet: ${pendentes.size} medição(ões) salva(s) neste aparelho, aguardando envio.",
                TomAviso.ATENCAO,
                acao = { BotaoSecundario("Enviar agora", viewModel::sincronizar) },
            )
        }

        CartaoSecao {
            Text("Nova medição", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
            if (grupo == Grupo.PRESSAO) {
                CampoTexto(valorA, viewModel::alterarA, "Pressão máxima (o número maior)", ajuda = "Exemplo: 120", teclado = KeyboardType.Number, sufixo = "mmHg")
                CampoTexto(valorB, viewModel::alterarB, "Pressão mínima (o número menor)", ajuda = "Exemplo: 80", teclado = KeyboardType.Number, acaoIme = ImeAction.Done, aoConfirmar = viewModel::registrar, sufixo = "mmHg")
            } else {
                CampoTexto(
                    valorA, viewModel::alterarA, if (grupo == Grupo.GLICEMIA) "Glicemia" else "Peso",
                    teclado = KeyboardType.Decimal, acaoIme = ImeAction.Done, aoConfirmar = viewModel::registrar, sufixo = grupo.unidade,
                )
            }
            BotaoPrincipal("Registrar medição", viewModel::registrar, carregando = salvando, icone = Icons.Filled.Add)
        }

        when (val r = resultado) {
            is Resultado.Enviado -> {
                val av = r.avaliacao
                when {
                    av?.nivel == Nivel.CRITICO || r.alertaDoServidor -> Aviso(
                        av?.mensagem ?: "Valor muito alto. Procure a UPA mais próxima.", TomAviso.ERRO,
                        acao = { BotaoPrincipal("Ver unidades de saúde", onVerUnidades, icone = Icons.Filled.Search) },
                    )
                    av?.nivel == Nivel.ATENCAO -> Aviso("Medição registrada. ${av.mensagem}", TomAviso.ATENCAO)
                    else -> Aviso("Medição registrada. ${av?.mensagem ?: ""}".trim(), TomAviso.SUCESSO)
                }
            }
            is Resultado.Pendente -> Aviso("Sem conexão. A medição foi salva neste aparelho e será enviada quando houver internet. ${r.avaliacao?.mensagem ?: ""}".trim(), TomAviso.ATENCAO)
            is Resultado.Erro -> Aviso(r.mensagem, TomAviso.ERRO)
            null -> Unit
        }

        TituloSecao("Seu histórico")
        ConteudoEstado(
            estado = historico,
            vazio = historico.dados?.linhas.isNullOrEmpty(),
            mensagemVazio = "Nenhuma medição ainda",
            textoVazio = "Registre a primeira medição acima. O gráfico aparece depois de duas.",
            onTentar = viewModel::carregar,
        ) { h ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val maiorSerie = h.series.maxOf { it.valores.size }
                if (!Grafico.poucosDados(maiorSerie)) {
                    CartaoSecao { GraficoLinhas(h.series, grupo.unidade) }
                } else {
                    Text("O gráfico aparece quando houver pelo menos duas medições.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                h.linhas.take(10).forEach { (data, texto) ->
                    CartaoSecao(modifier = Modifier.clearAndSetSemantics { contentDescription = Fala.medida(texto) + ", em " + data }) {
                        Text(texto, style = MaterialTheme.typography.titleSmall)
                        Text(data, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// Gráfico de linha com Canvas: escala nos dois eixos e legenda (sem biblioteca externa).
@Composable
private fun GraficoLinhas(series: List<Serie>, unidade: String) {
    val todos = series.flatMap { it.valores }
    val menor = todos.min()
    val maior = todos.max()
    val cores = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    val cinza = MaterialTheme.colorScheme.outline
    val referencia = series.first()

    // Quem não enxerga o desenho ouve um resumo: quantidade, valor mais recente, menor, maior e tendência.
    val resumo = series.joinToString(" ") { Fala.resumoSerie(it.nome, Fala.medida(unidade), it.valores) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics { contentDescription = resumo }) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            series.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = CircleShape, color = cores[i % cores.size], modifier = Modifier.size(12.dp)) {}
                    Text(s.nome + if (i == 1) " (tracejada)" else " (contínua)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row {
            Column(modifier = Modifier.height(160.dp).width(44.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(rotuloValor(maior), style = MaterialTheme.typography.bodySmall)
                Text(rotuloValor(menor), style = MaterialTheme.typography.bodySmall)
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                drawLine(cinza, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
                drawLine(cinza, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
                series.forEachIndexed { i, s ->
                    val pontos = if (maior == menor) s.valores.map { 0.5f } else s.valores.map { ((it - menor) / (maior - menor)).toFloat() }
                    if (pontos.size < 2) return@forEachIndexed
                    val passo = size.width / (pontos.size - 1)
                    val caminho = Path()
                    pontos.forEachIndexed { k, p ->
                        val x = k * passo
                        val y = size.height - p * size.height
                        if (k == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
                    }
                    // A segunda linha é tracejada: dá para diferenciar mesmo sem distinguir as cores.
                    drawPath(caminho, color = cores[i % cores.size], style = Stroke(width = 6f, pathEffect = if (i == 1) PathEffect.dashPathEffect(floatArrayOf(18f, 12f)) else null))
                    pontos.forEachIndexed { k, p -> drawCircle(color = cores[i % cores.size], radius = 8f, center = Offset(k * passo, size.height - p * size.height)) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(referencia.datas.first().substring(5, 10).split('-').reversed().joinToString("/"), style = MaterialTheme.typography.bodySmall)
            Text(referencia.datas.last().substring(5, 10).split('-').reversed().joinToString("/"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun rotuloValor(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)
