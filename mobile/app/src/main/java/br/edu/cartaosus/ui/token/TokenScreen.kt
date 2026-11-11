package br.edu.cartaosus.ui.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.cartaosus.data.AcessoResumo
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.SessionStore
import br.edu.cartaosus.security.TotpGenerator
import br.edu.cartaosus.ui.ConteudoEstado
import br.edu.cartaosus.ui.Estado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TokenViewModel(private val sessao: SessionStore, private val repositorio: CidadaoRepository) : ViewModel() {
    private val _segredo = MutableStateFlow(sessao.segredoDoUsuario())
    val segredo: StateFlow<String?> = _segredo.asStateFlow()

    private val _acessos = MutableStateFlow(Estado<List<AcessoResumo>>())
    val acessos: StateFlow<Estado<List<AcessoResumo>>> = _acessos.asStateFlow()

    init { carregarAcessos() }

    fun carregarAcessos() {
        _acessos.value = Estado(carregando = true)
        viewModelScope.launch {
            _acessos.value = try {
                Estado(carregando = false, dados = repositorio.acessos())
            } catch (e: Exception) {
                Estado(carregando = false, erro = Erros.mensagem(e))
            }
        }
    }

    // Segredo informado manualmente (conta criada em outro aparelho ou de demonstração).
    fun salvarSegredo(texto: String): Boolean {
        if (!TotpGenerator.segredoValido(texto)) return false
        val limpo = texto.trim().uppercase().replace(" ", "")
        sessao.salvarSegredo(sessao.idCidadao, limpo)
        _segredo.value = limpo
        return true
    }
}

@Composable
fun TokenScreen(viewModel: TokenViewModel) {
    val segredo by viewModel.segredo.collectAsState()
    val acessos by viewModel.acessos.collectAsState()
    val area = LocalClipboardManager.current
    var agoraMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var informando by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var invalido by remember { mutableStateOf(false) }

    // Atualiza o código a cada 250 ms: ao virar o passo de 30 s, o código muda sem recarregar a tela.
    LaunchedEffect(Unit) {
        while (true) {
            agoraMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(250)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Token de atendimento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Mostre este código ao atendente. Ele vale por 30 segundos.", style = MaterialTheme.typography.bodyMedium)

        val s = segredo
        if (s == null) {
            Text("Este aparelho ainda não tem o segredo TOTP desta conta.")
            Button(onClick = { informando = true }) { Text("Informar segredo") }
        } else {
            val codigo = TotpGenerator.codigo(s, agoraMs)
            val restante = TotpGenerator.restanteSegundos(agoraMs)
            Text(codigo.chunked(3).joinToString(" "), fontSize = 44.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { restante / 30f }, modifier = Modifier.fillMaxWidth())
            Text("Novo código em $restante s", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { area.setText(AnnotatedString(codigo)) }) { Text("Copiar código") }
        }

        Text("Últimos acessos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ConteudoEstado(
            estado = acessos,
            vazio = acessos.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhum acesso registrado.",
            onTentar = viewModel::carregarAcessos,
        ) { lista ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lista.take(10).forEach { a ->
                    Text("${a.dataHoraAcesso.take(16).replace('T', ' ')} · unidade ${a.idUnidade} · ${a.statusAutenticacao}${a.motivo?.let { " ($it)" } ?: ""}")
                }
            }
        }
    }

    if (informando) {
        AlertDialog(
            onDismissRequest = { informando = false },
            title = { Text("Segredo TOTP") },
            text = {
                Column {
                    Text("Cole o segredo mostrado no console da API ao cadastrar esta conta.")
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it; invalido = false },
                        isError = invalido,
                        supportingText = { if (invalido) Text("Segredo inválido: use letras A–Z e números 2–7.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.salvarSegredo(texto)) informando = false else invalido = true
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { informando = false }) { Text("Cancelar") } },
        )
    }
}
