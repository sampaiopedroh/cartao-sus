package br.edu.cartaosus.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Alvo de toque mínimo de 56 dp: confortável para quem tem pouca precisão motora.
private val AlturaBotao = 56.dp

@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
    icone: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !carregando,
        modifier = modifier.fillMaxWidth().heightIn(min = AlturaBotao),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (carregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(12.dp))
            Text("Aguarde...", style = MaterialTheme.typography.labelLarge)
        } else {
            if (icone != null) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = AlturaBotao),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotaoTexto(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

// Campo com rótulo sempre visível, ajuda e erro em texto (não só cor).
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    ajuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    acaoIme: ImeAction = ImeAction.Next,
    aoConfirmar: () -> Unit = {},
    senha: Boolean = false,
    senhaVisivel: Boolean = false,
    mascara: VisualTransformation = VisualTransformation.None,
    sufixo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = {
            val texto = erro ?: ajuda
            if (texto != null) Text(texto, style = MaterialTheme.typography.bodySmall)
        },
        suffix = if (sufixo != null) ({ Text(sufixo) }) else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = acaoIme),
        keyboardActions = KeyboardActions(onDone = { aoConfirmar() }, onSearch = { aoConfirmar() }),
        visualTransformation = if (senha && !senhaVisivel) PasswordVisualTransformation() else mascara,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { contentDescription = texto },
    )
}

enum class TomAviso { INFO, SUCESSO, ATENCAO, ERRO }

// Cartão de aviso: ícone + texto + cor, para não depender só da cor.
@Composable
fun Aviso(
    texto: String,
    tom: TomAviso,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
) {
    val (fundo, conteudo, icone) = when (tom) {
        TomAviso.INFO -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Filled.Info)
        TomAviso.SUCESSO -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Filled.CheckCircle)
        TomAviso.ATENCAO -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Filled.Warning)
        TomAviso.ERRO -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Filled.Warning)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fundo, contentColor = conteudo),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            acao?.invoke()
        }
    }
}

@Composable
fun EstadoCarregando(mensagem: String = "Carregando...", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 5.dp)
        Text(mensagem, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EstadoVazio(titulo: String, texto: String? = null, icone: ImageVector = Icons.Filled.Info, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (texto != null) Text(texto, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EstadoErro(mensagem: String, onTentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Aviso(mensagem, TomAviso.ERRO)
        BotaoPrincipal("Tentar de novo", onTentar, icone = Icons.Filled.Refresh)
    }
}

@Composable
fun CartaoSecao(
    modifier: Modifier = Modifier,
    corFundo: Color = MaterialTheme.colorScheme.surface,
    conteudo: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            conteudo()
        }
    }
}

@Composable
fun TelaCheia(conteudo: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { conteudo() }
}
