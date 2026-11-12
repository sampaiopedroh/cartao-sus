package br.edu.cartaosus.ui.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.CartaoSecao
import br.edu.cartaosus.ui.dashboard.dataAmigavel
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
import br.edu.cartaosus.domain.Fala
import br.edu.cartaosus.ui.comum.TituloSecao
import br.edu.cartaosus.ui.comum.anunciar
import br.edu.cartaosus.ui.comum.titulo
import androidx.compose.ui.semantics.clearAndSetSemantics
import br.edu.cartaosus.data.CidadaoRepository
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.data.SessionStore
import br.edu.cartaosus.security.TotpGenerator
import br.edu.cartaosus.ui.ConteudoEstado
import br.edu.cartaosus.ui.Estado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TokenViewModel(
    private val sessao: SessionStore,
    private val repositorio: CidadaoRepository,
    private val documentosDao: br.edu.cartaosus.data.DocumentoDao,
) : ViewModel() {
    val documentos: StateFlow<List<br.edu.cartaosus.data.DocumentoLocal>> =
        documentosDao.observar(sessao.idCidadao)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    // O compartilhamento só é liberado quando aparece um atendimento AUTORIZADO novo, depois que a tela abriu.
    private var base: Long? = null
    private val _liberado = MutableStateFlow(false)
    val liberado: StateFlow<Boolean> = _liberado.asStateFlow()

    private fun processar(lista: List<AcessoResumo>) {
        val b = base
        if (b == null) { base = lista.maxOfOrNull { it.idSessao } ?: 0L; return }
        _liberado.value = lista.any { it.idSessao > b && it.statusAutenticacao == "AUTORIZADO" }
    }

    fun atualizarSilencioso() {
        viewModelScope.launch {
            try {
                val lista = repositorio.acessos()
                _acessos.value = Estado(carregando = false, dados = lista)
                processar(lista)
            } catch (_: Exception) {}
        }
    }

    // Depois de compartilhar, a autorização é consumida: o próximo envio exige um novo código validado.
    fun encerrarCompartilhamento() {
        base = _acessos.value.dados?.maxOfOrNull { it.idSessao } ?: base
        _liberado.value = false
    }

    private val _segredo = MutableStateFlow(sessao.segredoDoUsuario())
    val segredo: StateFlow<String?> = _segredo.asStateFlow()

    private val _acessos = MutableStateFlow(Estado<List<AcessoResumo>>())
    val acessos: StateFlow<Estado<List<AcessoResumo>>> = _acessos.asStateFlow()

    init { carregarAcessos() }

    fun carregarAcessos() {
        _acessos.value = Estado(carregando = true)
        viewModelScope.launch {
            _acessos.value = try {
                val lista = repositorio.acessos()
                processar(lista)
                Estado(carregando = false, dados = lista)
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
    var copiado by remember { mutableStateOf(false) }
    val liberado by viewModel.liberado.collectAsState()
    val documentos by viewModel.documentos.collectAsState()
    var escolhendo by remember { mutableStateOf(false) }
    val contexto = androidx.compose.ui.platform.LocalContext.current

    // Confere a cada 5 segundos se o atendente já validou o código.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            viewModel.atualizarSilencioso()
        }
    }

    // Atualiza o código a cada 250 ms: ao virar o passo de 30 s, o código muda sem recarregar a tela.
    LaunchedEffect(Unit) {
        while (true) {
            agoraMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(250)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Mostre este código ao atendente no balcão. Seus dados só aparecem para ele com o seu código.",
            style = MaterialTheme.typography.bodyLarge,
        )

        val s = segredo
        if (s == null) {
            CartaoSecao {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text("Falta o código secreto", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
                }
                Text(
                    "Este aparelho ainda não tem o código secreto desta conta. Ele aparece uma vez quando você cria a conta.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                BotaoPrincipal("Informar código secreto", { informando = true })
            }
        } else {
            val codigo = TotpGenerator.codigo(s, agoraMs)
            val restante = TotpGenerator.restanteSegundos(agoraMs)
            val acabando = restante <= 5
            val corBarra = if (acabando) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            CartaoSecao(corFundo = MaterialTheme.colorScheme.primaryContainer) {
                Text("Seu código", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
                Text(
                    codigo.chunked(3).joinToString(" "),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    // Lido dígito por dígito; quando o código troca, o TalkBack avisa sozinho.
                    modifier = Modifier.fillMaxWidth().anunciar().semantics { contentDescription = "Seu código é " + Fala.soletrar(codigo) },
                )
                LinearProgressIndicator(
                    progress = { restante / 30f },
                    color = corBarra,
                    trackColor = MaterialTheme.colorScheme.surface,
                    // A barra é só visual; o tempo restante é falado pelo texto logo abaixo.
                    modifier = Modifier.fillMaxWidth().height(12.dp).clearAndSetSemantics {},
                )
                Text(
                    if (acabando) "Vai mudar em $restante segundos" else "Muda em $restante segundos",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (acabando) FontWeight.Bold else FontWeight.Normal,
                )
            }
            BotaoSecundario(
                modifier = Modifier.anunciar(),
                texto = if (copiado) "Código copiado" else "Copiar código",
                onClick = { area.setText(AnnotatedString(codigo)); copiado = true },
            )
            LaunchedEffect(restante) { if (restante == 30) copiado = false }

            if (liberado) {
                br.edu.cartaosus.ui.comum.Aviso(
                    "O atendente validou o seu código. Agora você pode mostrar seus documentos.",
                    br.edu.cartaosus.ui.comum.TomAviso.SUCESSO,
                )
                BotaoPrincipal("Compartilhar documentos", { escolhendo = true })
            } else {
                Text(
                    "Depois que o atendente validar o seu código, aparece aqui a opção de compartilhar documentos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TituloSecao("Últimos atendimentos")
        ConteudoEstado(
            estado = acessos,
            vazio = acessos.dados.isNullOrEmpty(),
            mensagemVazio = "Nenhum atendimento ainda",
            textoVazio = "Quando você usar seu código no balcão, o registro aparece aqui.",
            onTentar = viewModel::carregarAcessos,
        ) { lista ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lista.take(8).forEach { a -> LinhaAcesso(a) }
            }
        }
    }

    if (escolhendo) {
        br.edu.cartaosus.ui.documentos.DialogoCompartilhar(
            documentos = documentos,
            onCompartilhar = { escolhidos ->
                escolhendo = false
                br.edu.cartaosus.ui.documentos.compartilharDocumentos(contexto, escolhidos)
                viewModel.encerrarCompartilhamento()
            },
            onFechar = { escolhendo = false },
        )
    }

    if (informando) {
        AlertDialog(
            onDismissRequest = { informando = false },
            title = { Text("Código secreto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cole aqui o código secreto que apareceu quando você criou a conta.", style = MaterialTheme.typography.bodyLarge)
                    CampoTexto(
                        valor = texto,
                        aoMudar = { texto = it; invalido = false },
                        rotulo = "Código secreto",
                        erro = if (invalido) "Use só letras de A a Z e números de 2 a 7 (mínimo 16)." else null,
                        acaoIme = ImeAction.Done,
                        aoConfirmar = { if (viewModel.salvarSegredo(texto)) informando = false else invalido = true },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { if (viewModel.salvarSegredo(texto)) informando = false else invalido = true }) {
                    Text("Salvar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = { TextButton(onClick = { informando = false }) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

@Composable
private fun LinhaAcesso(a: AcessoResumo) {
    val ok = a.statusAutenticacao == "AUTORIZADO"
    Card(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = (if (ok) "Atendimento autorizado" else "Tentativa recusada") +
                " em ${dataAmigavel(a.dataHoraAcesso)}, unidade ${a.idUnidade}"
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(if (ok) "Atendimento autorizado" else "Tentativa recusada", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${dataAmigavel(a.dataHoraAcesso)} · unidade ${a.idUnidade}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
