package br.edu.cartaosus.ui.consulta

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.CatalogoItem
import br.edu.cartaosus.data.EquipamentosResposta
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.MedicamentoItem
import br.edu.cartaosus.data.SusRepository
import br.edu.cartaosus.data.UnidadeResumo
import br.edu.cartaosus.domain.Distancia
import br.edu.cartaosus.ui.ConteudoEstado
import br.edu.cartaosus.ui.Estado
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.BotaoTexto
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.CartaoSecao
import br.edu.cartaosus.ui.comum.EstadoCarregando
import br.edu.cartaosus.ui.comum.EstadoErro
import br.edu.cartaosus.ui.comum.TomAviso
import br.edu.cartaosus.ui.dashboard.dataAmigavel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnidadeComDistancia(val unidade: UnidadeResumo, val km: Double?)

class ConsultaViewModel(private val sus: SusRepository) : ViewModel() {
    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca.asStateFlow()

    private val _catalogo = MutableStateFlow(Estado<List<CatalogoItem>>())
    val catalogo: StateFlow<Estado<List<CatalogoItem>>> = _catalogo.asStateFlow()

    private val _escolhido = MutableStateFlow<CatalogoItem?>(null)
    val escolhido: StateFlow<CatalogoItem?> = _escolhido.asStateFlow()

    private val _estoque = MutableStateFlow(Estado<List<MedicamentoItem>>(carregando = false))
    val estoque: StateFlow<Estado<List<MedicamentoItem>>> = _estoque.asStateFlow()

    private val _unidades = MutableStateFlow(Estado<List<UnidadeComDistancia>>())
    val unidades: StateFlow<Estado<List<UnidadeComDistancia>>> = _unidades.asStateFlow()

    private val _filtro = MutableStateFlow<String?>(null)
    val filtro: StateFlow<String?> = _filtro.asStateFlow()

    private val _equipamentos = MutableStateFlow<Estado<EquipamentosResposta>?>(null)
    val equipamentos: StateFlow<Estado<EquipamentosResposta>?> = _equipamentos.asStateFlow()

    init {
        buscarRemedio()
        carregarUnidades()
    }

    fun alterarBusca(valor: String) { _busca.value = valor }

    // Sem texto, lista os remédios mais comuns do catálogo (não precisa digitar nada).
    fun buscarRemedio() {
        _catalogo.value = Estado(carregando = true)
        _escolhido.value = null
        _estoque.value = Estado(carregando = false)
        viewModelScope.launch {
            _catalogo.value = try {
                Estado(carregando = false, dados = sus.catalogo(_busca.value.trim()))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun escolherRemedio(item: CatalogoItem) {
        _escolhido.value = item
        consultarEstoque()
    }

    fun consultarEstoque() {
        val item = _escolhido.value ?: return
        _estoque.value = Estado(carregando = true)
        viewModelScope.launch {
            _estoque.value = try {
                Estado(carregando = false, dados = sus.estoque(item.catmat))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun voltarParaLista() {
        _escolhido.value = null
        _estoque.value = Estado(carregando = false)
    }

    fun filtrar(tipo: String?) { _filtro.value = tipo }

    fun carregarUnidades() {
        _unidades.value = Estado(carregando = true)
        viewModelScope.launch {
            _unidades.value = try {
                val lista = sus.unidades().map {
                    val km = if (it.latitude != null && it.longitude != null)
                        Distancia.km(Distancia.CENTRO_SP_LAT, Distancia.CENTRO_SP_LON, it.latitude, it.longitude) else null
                    UnidadeComDistancia(it, km)
                }.sortedBy { it.km ?: Double.MAX_VALUE }
                Estado(carregando = false, dados = lista)
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun verEquipamentos(cnes: String) {
        _equipamentos.value = Estado(carregando = true)
        viewModelScope.launch {
            _equipamentos.value = try {
                Estado(carregando = false, dados = sus.equipamentos(cnes))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun fecharEquipamentos() { _equipamentos.value = null }
}

private val FILTROS = listOf<Pair<String?, String>>(null to "Todas", "UBS" to "Postos (UBS)", "UPA" to "UPA", "HOSPITAL" to "Hospitais")

@Composable
fun ConsultaScreen(viewModel: ConsultaViewModel) {
    var aba by remember { mutableIntStateOf(0) }
    val equipamentos by viewModel.equipamentos.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Remédios", style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.padding(vertical = 6.dp))
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Unidades de saúde", style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.padding(vertical = 6.dp))
        }
        if (aba == 0) AbaRemedios(viewModel) else AbaUnidades(viewModel)
    }

    equipamentos?.let { estado ->
        AlertDialog(
            onDismissRequest = viewModel::fecharEquipamentos,
            title = { Text(estado.dados?.estabelecimento ?: "Equipamentos") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        estado.carregando -> EstadoCarregando("Consultando o CNES...")
                        estado.erro != null -> Aviso(estado.erro, TomAviso.ERRO)
                        estado.dados == null || estado.dados.equipamentos.isEmpty() ->
                            Text("Esta unidade não tem equipamentos registrados no CNES.", style = MaterialTheme.typography.bodyLarge)
                        else -> estado.dados.equipamentos.take(15).forEach { e ->
                            Text("• ${e.descricao}: ${e.quantidadeExistente ?: 0} (em uso: ${e.quantidadeEmUso ?: 0})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::fecharEquipamentos) { Text("Fechar", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

@Composable
private fun AbaRemedios(viewModel: ConsultaViewModel) {
    val busca by viewModel.busca.collectAsState()
    val catalogo by viewModel.catalogo.collectAsState()
    val escolhido by viewModel.escolhido.collectAsState()
    val estoque by viewModel.estoque.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val item = escolhido
        if (item == null) {
            Text("Veja em quais lugares da cidade de São Paulo o remédio está em estoque.", style = MaterialTheme.typography.bodyLarge)
            CampoTexto(
                valor = busca, aoMudar = viewModel::alterarBusca, rotulo = "Nome do remédio",
                ajuda = "Exemplo: insulina", acaoIme = ImeAction.Search, aoConfirmar = viewModel::buscarRemedio,
            )
            BotaoSecundario("Buscar", viewModel::buscarRemedio, icone = Icons.Filled.Search)

            Text(if (busca.isBlank()) "Remédios do catálogo" else "Resultado da busca", style = MaterialTheme.typography.titleMedium)
            ConteudoEstado(
                estado = catalogo,
                vazio = catalogo.dados.isNullOrEmpty(),
                mensagemVazio = "Nenhum remédio com esse nome",
                textoVazio = "Confira a escrita ou apague o texto para ver a lista completa.",
                onTentar = viewModel::buscarRemedio,
            ) { lista ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lista.forEach { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.escolherRemedio(r) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(r.nome, style = MaterialTheme.typography.titleSmall)
                                Text(r.apresentacao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Toque para ver onde tem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            BotaoTexto("← Voltar para a lista", viewModel::voltarParaLista)
            Text(item.nome, style = MaterialTheme.typography.headlineSmall)
            Text(item.apresentacao, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConteudoEstado(
                estado = estoque,
                vazio = estoque.dados.isNullOrEmpty(),
                mensagemVazio = "Nenhum estoque encontrado",
                textoVazio = "Este remédio não aparece em estoque na cidade agora. Pergunte na unidade de saúde.",
                onTentar = viewModel::consultarEstoque,
            ) { lista ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Aviso("Estes dados são informados pelas unidades ao Ministério da Saúde e podem não estar atualizados no minuto. Confirme antes de ir.", TomAviso.INFO)
                    lista.take(10).forEach { m ->
                        CartaoSecao {
                            Text(m.estabelecimento ?: "Estabelecimento não informado", style = MaterialTheme.typography.titleSmall)
                            Text("Em estoque: ${m.quantidade?.toLong() ?: 0} unidades", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Posição informada em ${m.dataPosicao?.let { dataAmigavel(it + "T00:00:00").substringBefore(" às") } ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbaUnidades(viewModel: ConsultaViewModel) {
    val unidades by viewModel.unidades.collectAsState()
    val filtro by viewModel.filtro.collectAsState()
    val contexto = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Da mais perto para a mais longe, a partir do centro de São Paulo.", style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTROS.forEach { (tipo, rotulo) ->
                FilterChip(selected = filtro == tipo, onClick = { viewModel.filtrar(tipo) }, label = { Text(rotulo, style = MaterialTheme.typography.labelMedium) })
            }
        }
        ConteudoEstado(
            estado = unidades,
            vazio = unidades.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhuma unidade cadastrada",
            onTentar = viewModel::carregarUnidades,
        ) { todas ->
            val lista = todas.filter { filtro == null || it.unidade.tipo == filtro }
            if (lista.isEmpty()) {
                Text("Nenhuma unidade desse tipo.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lista.forEach { item ->
                        CartaoSecao {
                            Text(item.unidade.nome, style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "${rotuloTipo(item.unidade.tipo)} · ${item.unidade.bairro.lowercase().replaceFirstChar { it.uppercase() }}" +
                                        (item.km?.let { " · %.1f km".format(it) } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    BotaoSecundario("Como chegar", {
                                        val lat = item.unidade.latitude
                                        val lon = item.unidade.longitude
                                        if (lat != null && lon != null) {
                                            contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(item.unidade.nome)})")))
                                        }
                                    })
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    BotaoSecundario("Equipamentos", { viewModel.verEquipamentos(item.unidade.codigoCnes) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rotuloTipo(tipo: String) = when (tipo) {
    "UBS" -> "Posto de saúde (UBS)"
    "UPA" -> "UPA 24 horas"
    "HOSPITAL" -> "Hospital"
    else -> tipo
}

