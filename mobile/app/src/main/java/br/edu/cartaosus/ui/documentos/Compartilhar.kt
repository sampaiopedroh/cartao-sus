package br.edu.cartaosus.ui.documentos

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.cartaosus.data.DocumentoLocal
import br.edu.cartaosus.domain.TipoDocumento

// Abre o compartilhamento do Android com os arquivos escolhidos. O app já tem permissão de leitura de cada
// arquivo (escolhido pelo usuário); aqui ela é repassada só para quem receber, e só para estes arquivos.
fun compartilharDocumentos(context: Context, docs: List<DocumentoLocal>) {
    // Só segue com arquivos que ainda podem ser lidos; os outros (apagados ou sem permissão) são avisados.
    val legiveis = docs.filter { d ->
        try { context.contentResolver.openInputStream(Uri.parse(d.uri))?.use { true } ?: false } catch (e: Exception) { false }
    }
    if (legiveis.size < docs.size) {
        Toast.makeText(context, "Alguns arquivos não foram encontrados e não serão compartilhados.", Toast.LENGTH_LONG).show()
    }
    if (legiveis.isEmpty()) return
    val uris = ArrayList(legiveis.map { Uri.parse(it.uri) })
    try {
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0]) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = ClipData.newRawUri("Documentos", uris[0]).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar documentos"))
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível compartilhar agora.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DialogoCompartilhar(
    documentos: List<DocumentoLocal>,
    onCompartilhar: (List<DocumentoLocal>) -> Unit,
    onFechar: () -> Unit,
) {
    val marcados = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Quais documentos mostrar?") },
        text = {
            if (documentos.isEmpty()) {
                Text("Você ainda não anexou documentos. Anexe em \"Meus documentos\", na tela inicial.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                ) {
                    Text("Escolha só o que o atendente precisa ver.", style = MaterialTheme.typography.bodyLarge)
                    documentos.forEach { d ->
                        val marcado = d.id in marcados
                        val alternar = { if (marcado) marcados.remove(d.id) else marcados.add(d.id); Unit }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable(onClick = alternar).padding(vertical = 4.dp),
                        ) {
                            Checkbox(checked = marcado, onCheckedChange = { alternar() })
                            Column {
                                Text(d.titulo, style = MaterialTheme.typography.titleSmall)
                                Text(TipoDocumento.de(d.tipo).rotulo, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (documentos.isNotEmpty()) {
                TextButton(
                    enabled = marcados.isNotEmpty(),
                    onClick = { onCompartilhar(documentos.filter { it.id in marcados }) },
                ) { Text("Compartilhar", style = MaterialTheme.typography.labelLarge) }
            }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text(if (documentos.isEmpty()) "Fechar" else "Cancelar", style = MaterialTheme.typography.labelLarge) } },
    )
}
