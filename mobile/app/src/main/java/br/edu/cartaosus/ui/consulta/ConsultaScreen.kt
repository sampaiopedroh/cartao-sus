package br.edu.cartaosus.ui.consulta

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnidadeComDistancia(val unidade: UnidadeResumo, val km: Double?)

class ConsultaViewModel(private val sus: SusRepository) : ViewModel() {
    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca.asStateFlow()

    private val _catalogo = MutableStateFlow(Estado<List<CatalogoItem>>(carregando = false))
    val catalogo: StateFlow<Estado<List<CatalogoItem>>> = _catalogo.asStateFlow()

    private val _estoque = MutableStateFlow(Estado<List<MedicamentoItem>>(carregando = false))
    val estoque: StateFlow<Estado<List<MedicamentoItem>>> = _estoque.asStateFlow()

    private val _unidades = MutableStateFlow(Estado<List<UnidadeComDistancia>>())
    val unidades: StateFlow<Estado<List<UnidadeComDistancia>>> = _unidades.asStateFlow()

    private val _equipamentos = MutableStateFlow(Estado<EquipamentosResposta>(carregando = false))
    val equipamentos: StateFlow<Estado<EquipamentosResposta>> = _equipamentos.asStateFlow()

    init { carregarUnidades() }

    fun alterarBusca(valor: String) { _busca.value = valor }

    fun buscarRemedio() {
        val termo = _busca.value.trim()
        if (termo.isEmpty()) return
        _catalogo.value = Estado(carregando = true)
        _estoque.value = Estado(carregando = false)
        viewModelScope.launch {
            _catalogo.value = try {
                Estado(carregando = false, dados = sus.catalogo(termo))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    fun escolherRemedio(item: CatalogoItem) {
        _estoque.value = Estado(carregando = true)
        viewModelScope.launch {
            _estoque.value = try {
                Estado(carregando = false, dados = sus.estoque(item.catmat))
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

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
}

@Composable
fun ConsultaScreen(viewModel: ConsultaViewModel) {
    val busca by viewModel.busca.collectAsState()
    val catalogo by viewModel.catalogo.collectAsState()
    val estoque by viewModel.estoque.collectAsState()
    val unidades by viewModel.unidades.collectAsState()
    val equipamentos by viewModel.equipamentos.collectAsState()
    val contexto = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Consulta SUS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Remédios em estoque na cidade de São Paulo", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = busca,
            onValueChange = viewModel::alterarBusca,
            label = { Text("Nome do remédio (ex.: insulina)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::buscarRemedio, modifier = Modifier.fillMaxWidth()) { Text("Buscar") }

        if (catalogo.dados != null || catalogo.carregando || catalogo.erro != null) {
            ConteudoEstado(
                estado = catalogo,
                vazio = catalogo.dados.isNullOrEmpty(),
                mensagemVazio = "Nenhum medicamento encontrado com esse nome.",
                onTentar = viewModel::buscarRemedio,
            ) { lista ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lista.forEach { item ->
                        TextButton(onClick = { viewModel.escolherRemedio(item) }) {
                            Text("${item.nome} — ${item.apresentacao}")
                        }
                    }
                }
            }
        }

        if (estoque.carregando || estoque.erro != null || estoque.dados != null) {
            Text("Estoque", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ConteudoEstado(
                estado = estoque,
                vazio = estoque.dados.isNullOrEmpty(),
                mensagemVazio = "Nenhum estoque encontrado nesta cidade.",
                onTentar = { /* refaz a consulta do item escolhido: o usuário toca novamente */ },
            ) { lista ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lista.take(10).forEach { m ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(m.nome, fontWeight = FontWeight.Bold)
                                Text("Quantidade: ${m.quantidade?.toLong() ?: 0} · posição ${m.dataPosicao ?: "-"}")
                                Text(m.estabelecimento ?: "Estabelecimento não informado")
                            }
                        }
                    }
                }
            }
        }

        Text("Unidades próximas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ConteudoEstado(
            estado = unidades,
            vazio = unidades.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhuma unidade cadastrada.",
            onTentar = viewModel::carregarUnidades,
        ) { lista ->
            // Coluna simples (não LazyColumn): a tela já rola, e LazyColumn aninhado quebra o layout.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lista.forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.unidade.nome, fontWeight = FontWeight.Bold)
                            Text("${item.unidade.tipo} · ${item.unidade.bairro}" + (item.km?.let { " · %.1f km".format(it) } ?: ""))
                            Row2(
                                onEquipamentos = { viewModel.verEquipamentos(item.unidade.codigoCnes) },
                                onMapa = {
                                    val lat = item.unidade.latitude
                                    val lon = item.unidade.longitude
                                    if (lat != null && lon != null) {
                                        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(item.unidade.nome)})")))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (equipamentos.carregando || equipamentos.erro != null || equipamentos.dados != null) {
            Text("Equipamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ConteudoEstado(
                estado = equipamentos,
                vazio = equipamentos.dados?.equipamentos.isNullOrEmpty(),
                mensagemVazio = "Sem equipamentos registrados no CNES.",
                onTentar = { },
            ) { r ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.estabelecimento ?: r.coCnes, fontWeight = FontWeight.Bold)
                    r.equipamentos.take(12).forEach { e ->
                        Text("${e.descricao}: ${e.quantidadeExistente ?: 0} (em uso: ${e.quantidadeEmUso ?: 0})")
                    }
                }
            }
        }
    }
}

@Composable
private fun Row2(onEquipamentos: () -> Unit, onMapa: () -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onEquipamentos) { Text("Equipamentos") }
        TextButton(onClick = onMapa) { Text("Ver no mapa") }
    }
}
