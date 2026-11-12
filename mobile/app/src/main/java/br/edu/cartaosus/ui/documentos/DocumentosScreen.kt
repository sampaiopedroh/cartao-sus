package br.edu.cartaosus.ui.documentos

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.collectAsState
import br.edu.cartaosus.data.DocumentoDao
import br.edu.cartaosus.data.DocumentoLocal
import br.edu.cartaosus.domain.TipoDocumento
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.EstadoVazio
import br.edu.cartaosus.ui.comum.TomAviso
import br.edu.cartaosus.ui.comum.grupo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.edu.cartaosus.ui.dashboard.dataAmigavel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Rascunho do diálogo: documento novo (id = 0, com uri) ou edição de um existente.
data class Rascunho(val id: Long, val uri: String, val tipo: TipoDocumento, val titulo: String, val adicionadoEm: Long)

// Os documentos ficam só no celular: guardamos o caminho (URI) e a permissão de leitura dada pelo usuário.
class DocumentosViewModel(
    private val dao: DocumentoDao,
    private val idCidadao: () -> Long,
    private val resolver: ContentResolver,
) : ViewModel() {
    val documentos: StateFlow<List<DocumentoLocal>> =
        dao.observar(idCidadao()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ausentes = MutableStateFlow<Set<Long>>(emptySet())
    val ausentes: StateFlow<Set<Long>> = _ausentes.asStateFlow()

    fun verificar(lista: List<DocumentoLocal>) {
        viewModelScope.launch {
            val faltando = withContext(Dispatchers.IO) { lista.filter { !arquivoExiste(it.uri) }.map { it.id }.toSet() }
            _ausentes.value = faltando
        }
    }

    private fun arquivoExiste(uri: String): Boolean = try {
        resolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    fun salvar(rascunho: Rascunho) {
        viewModelScope.launch {
            val titulo = rascunho.titulo.trim().ifBlank { rascunho.tipo.rotulo }
            if (rascunho.id == 0L) {
                dao.inserir(DocumentoLocal(idCidadao = idCidadao(), tipo = rascunho.tipo.name, titulo = titulo,
                    uri = rascunho.uri, adicionadoEm = rascunho.adicionadoEm))
            } else {
                dao.atualizar(DocumentoLocal(rascunho.id, idCidadao(), rascunho.tipo.name, titulo, rascunho.uri, rascunho.adicionadoEm))
            }
        }
    }

    fun remover(documento: DocumentoLocal) {
        viewModelScope.launch {
            dao.remover(documento)
            // Devolve a permissão de leitura que o Android guardava para este arquivo.
            try { resolver.releasePersistableUriPermission(Uri.parse(documento.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
    }
}

private fun nomeDoArquivo(resolver: ContentResolver, uri: Uri): String? = try {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
} catch (e: Exception) { null }

private fun abrirArquivo(context: Context, doc: DocumentoLocal) {
    val uri = Uri.parse(doc.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Não há aplicativo para abrir este arquivo.", Toast.LENGTH_LONG).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, "O Android não permite mais abrir este arquivo. Remova e anexe de novo.", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentosScreen(viewModel: DocumentosViewModel) {
    val context = LocalContext.current
    val documentos by viewModel.documentos.collectAsState()
    val ausentes by viewModel.ausentes.collectAsState()
    var rascunho by remember { mutableStateOf<Rascunho?>(null) }
    var removendo by remember { mutableStateOf<DocumentoLocal?>(null) }

    androidx.compose.runtime.LaunchedEffect(documentos) { viewModel.verificar(documentos) }

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Pede ao Android para lembrar a permissão de leitura mesmo depois de fechar o app.
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
            val nome = nomeDoArquivo(context.contentResolver, uri)
            rascunho = Rascunho(0, uri.toString(), TipoDocumento.OUTRO,
                TipoDocumento.tituloSugerido(nome, TipoDocumento.OUTRO), System.currentTimeMillis())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Guarde aqui o caminho dos seus documentos (carterinha, RG, CPF, laudos, receitas). " +
                "Os arquivos continuam no seu celular: nada é enviado para a internet.",
            style = MaterialTheme.typography.bodyLarge,
        )
        BotaoPrincipal(
            texto = "Anexar documento",
            icone = Icons.Filled.Add,
            onClick = { seletor.launch(arrayOf("application/pdf", "image/*")) },
        )
        if (documentos.isEmpty()) {
            EstadoVazio("Nenhum documento anexado", "Toque em \"Anexar documento\" e escolha um PDF ou uma foto no seu celular.")
        } else {
            documentos.forEach { doc ->
                val sumiu = doc.id in ausentes
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Column(Modifier.grupo(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(TipoDocumento.de(doc.tipo).rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(doc.titulo, style = MaterialTheme.typography.titleMedium)
                        Text("Anexado em " + SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(doc.adicionadoEm)),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                        if (sumiu) {
                            Aviso("Não encontramos este arquivo no celular. Ele pode ter sido apagado ou movido.", TomAviso.ATENCAO)
                        }
                        Spacer(Modifier.height(4.dp))
                        if (!sumiu) BotaoPrincipal(texto = "Abrir", onClick = { abrirArquivo(context, doc) }, descricao = "Abrir ${doc.titulo}")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(modifier = Modifier.semantics { contentDescription = "Editar ${doc.titulo}" }, onClick = {
                                rascunho = Rascunho(doc.id, doc.uri, TipoDocumento.de(doc.tipo), doc.titulo, doc.adicionadoEm)
                            }) { Text("Editar", style = MaterialTheme.typography.labelLarge) }
                            TextButton(modifier = Modifier.semantics { contentDescription = "Remover ${doc.titulo}" }, onClick = { removendo = doc }) { Text("Remover", style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
            }
        }
    }

    rascunho?.let { r ->
        AlertDialog(
            onDismissRequest = { rascunho = null },
            title = { Text(if (r.id == 0L) "Que documento é este?" else "Editar documento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipoDocumento.entries.forEach { tipo ->
                            FilterChip(
                                selected = r.tipo == tipo,
                                onClick = { rascunho = r.copy(tipo = tipo) },
                                label = { Text(tipo.rotulo, style = MaterialTheme.typography.labelLarge) },
                            )
                        }
                    }
                    CampoTexto(valor = r.titulo, aoMudar = { rascunho = r.copy(titulo = it) }, rotulo = "Nome do documento")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.salvar(r); rascunho = null }) { Text("Salvar", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { rascunho = null }) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
        )
    }

    removendo?.let { doc ->
        AlertDialog(
            onDismissRequest = { removendo = null },
            title = { Text("Remover documento?") },
            text = { Text("Só some daqui. O arquivo continua guardado no seu celular.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { viewModel.remover(doc); removendo = null }) { Text("Remover", style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = { removendo = null }) { Text("Manter", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}
